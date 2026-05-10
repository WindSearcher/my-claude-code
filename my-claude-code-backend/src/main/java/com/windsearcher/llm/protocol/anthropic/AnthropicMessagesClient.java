package com.windsearcher.llm.protocol.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.windsearcher.exception.LlmApiException;
import com.windsearcher.llm.LlmStreamSink;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Anthropic Messages HTTP 客户端 — 对应官方 {@code POST /v1/messages} 与 SSE 流，
 * 对齐 {@code services/api/client.ts} 的直连 Anthropic 路径（x-api-key + anthropic-version）。
 */
@Slf4j
public class AnthropicMessagesClient {

    public static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final MediaType JSON = MediaType.parse("application/json");

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public AnthropicMessagesClient(ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public JsonNode createBlocking(String baseUrl, String apiKey, ObjectNode body, long timeoutMs) throws IOException {
        Request request = buildRequest(baseUrl, apiKey, body, false);
        OkHttpClient client = httpClient.newBuilder()
                .callTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody rb = response.body();
            String text = rb != null ? rb.string() : "";
            if (!response.isSuccessful()) {
                throw apiException(response.code(), text, true);
            }
            return objectMapper.readTree(text);
        }
    }

    /**
     * 流式请求；在当前线程阻塞直到流结束或出错。
     */
    public void createStreaming(String baseUrl, String apiKey, ObjectNode body, long timeoutMs, LlmStreamSink sink) {
        ObjectNode streamed = body.deepCopy();
        streamed.put("stream", true);
        Request request = buildRequest(baseUrl, apiKey, streamed, true);
        OkHttpClient client = httpClient.newBuilder()
                .callTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                sink.onError("Anthropic HTTP " + response.code() + ": " + err, response.code() >= 500);
                sink.onMessageComplete("error");
                return;
            }
            ResponseBody bodyStream = response.body();
            if (bodyStream == null) {
                sink.onError("Empty Anthropic stream body", true);
                sink.onMessageComplete("error");
                return;
            }
            parseSseStream(bodyStream, sink);
        } catch (IOException e) {
            log.warn("Anthropic stream IO: {}", e.getMessage());
            sink.onError(e.getMessage(), true);
            sink.onMessageComplete("error");
        }
    }

    private Request buildRequest(String baseUrl, String apiKey, ObjectNode jsonBody, boolean stream) {
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        url = url + "/v1/messages";
        Request.Builder b = new Request.Builder()
                .url(url)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json");
        if (stream) {
            b.header("Accept", "text/event-stream");
        }
        return b.post(RequestBody.create(jsonBody.toString(), JSON)).build();
    }

    private void parseSseStream(ResponseBody bodyStream, LlmStreamSink sink) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(bodyStream.byteStream(), StandardCharsets.UTF_8));
        StringBuilder dataBuffer = new StringBuilder();
        Map<Integer, ToolStreamState> toolsByIndex = new HashMap<>();
        String stopReason = "end_turn";

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data:")) {
                if (!dataBuffer.isEmpty()) {
                    dataBuffer.append('\n');
                }
                dataBuffer.append(line.substring(5).trim());
            } else if (line.isEmpty() && !dataBuffer.isEmpty()) {
                String payload = dataBuffer.toString();
                dataBuffer.setLength(0);
                if ("[DONE]".equals(payload)) {
                    break;
                }
                try {
                    JsonNode event = objectMapper.readTree(payload);
                    String type = event.path("type").asText("");
                    switch (type) {
                        case "content_block_start" -> {
                            int index = event.path("index").asInt(-1);
                            JsonNode block = event.get("content_block");
                            if (block != null && index >= 0) {
                                String bt = block.path("type").asText("");
                                if ("tool_use".equals(bt)) {
                                    ToolStreamState st = new ToolStreamState(
                                            block.path("id").asText(""),
                                            block.path("name").asText(""));
                                    toolsByIndex.put(index, st);
                                    sink.onToolCallStart(st.id, st.name);
                                }
                            }
                        }
                        case "content_block_delta" -> {
                            int index = event.path("index").asInt(-1);
                            JsonNode delta = event.get("delta");
                            if (delta == null) {
                                break;
                            }
                            String dt = delta.path("type").asText("");
                            if ("text_delta".equals(dt)) {
                                String t = delta.path("text").asText("");
                                if (!t.isEmpty()) {
                                    sink.onTextDelta(t);
                                }
                            } else if ("thinking_delta".equals(dt)) {
                                String t = delta.path("thinking").asText("");
                                if (!t.isEmpty()) {
                                    sink.onReasoningDelta(t);
                                }
                            } else if ("input_json_delta".equals(dt)) {
                                String fragment = delta.path("partial_json").asText("");
                                ToolStreamState st = toolsByIndex.get(index);
                                if (st != null && !fragment.isEmpty()) {
                                    sink.onToolCallArgumentsDelta(st.id, fragment);
                                }
                            }
                        }
                        case "message_delta" -> {
                            JsonNode d = event.get("delta");
                            if (d != null) {
                                String sr = d.path("stop_reason").asText(null);
                                if (sr != null && !sr.isEmpty()) {
                                    stopReason = mapStopReason(sr);
                                }
                                JsonNode u = d.get("usage");
                                if (u != null) {
                                    int inTok = u.path("input_tokens").asInt(0);
                                    int outTok = u.path("output_tokens").asInt(0);
                                    if (inTok > 0 || outTok > 0) {
                                        sink.onUsage(inTok, outTok);
                                    }
                                }
                            }
                        }
                        case "message_stop" -> {
                            sink.onMessageComplete(stopReason);
                            return;
                        }
                        case "error" -> {
                            String msg = event.path("error").path("message").asText(event.toString());
                            sink.onError(msg, true);
                            sink.onMessageComplete("error");
                            return;
                        }
                        default -> {
                            // ping, message_start, content_block_stop 等忽略
                        }
                    }
                } catch (Exception parseEx) {
                    log.debug("Skip malformed SSE JSON: {}", parseEx.getMessage());
                }
            }
        }
        sink.onMessageComplete(stopReason);
    }

    private static String mapStopReason(String api) {
        return switch (Objects.requireNonNullElse(api, "")) {
            case "max_tokens" -> "max_tokens";
            case "tool_use" -> "tool_use";
            case "stop_sequence" -> "stop_sequence";
            case "end_turn" -> "end_turn";
            default -> api.isEmpty() ? "end_turn" : api;
        };
    }

    private static LlmApiException apiException(int code, String body, boolean retryable) {
        return new LlmApiException("Anthropic API HTTP " + code + ": " + body, retryable, code);
    }

    private static final class ToolStreamState {
        final String id;
        final String name;

        ToolStreamState(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
