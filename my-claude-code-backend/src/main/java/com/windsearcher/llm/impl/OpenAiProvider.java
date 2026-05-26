package com.windsearcher.llm.impl;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.windsearcher.domain.ChatRequest;
import com.windsearcher.domain.ChatResponse;
import com.windsearcher.domain.TokenUsage;
import com.windsearcher.domain.ToolCall;
import com.windsearcher.exception.LlmApiException;
import com.windsearcher.llm.LlmProvider;
import com.windsearcher.llm.LlmStreamSink;
import com.windsearcher.llm.protocol.openai.OpenAiChatRequestFactory;
import com.windsearcher.llm.util.UsageParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * OpenAI Chat Completions 兼容协议 — HTTP、SSE 与 {@link ChatResponse} 装配均在本类完成；
 * 请求体由 {@link OpenAiChatRequestFactory} 构建。
 */
@Slf4j
public class OpenAiProvider implements LlmProvider {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json");
    private static final long DEFAULT_TIMEOUT_MS = 600000L;

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final String providerName;
    private final List<String> supportedModels;
    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;

    public OpenAiProvider(
            String providerName,
            ObjectMapper objectMapper,
            String apiKey,
            String baseUrl,
            String defaultModel,
            List<String> supportedModels) {
        this.providerName = providerName;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.supportedModels = supportedModels != null ? supportedModels : List.of();
        this.defaultModel = defaultModel;
        this.httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(20, 20, java.util.concurrent.TimeUnit.SECONDS))
                .connectTimeout(Duration.ofSeconds(300))
                .readTimeout(Duration.ofMinutes(5))
                .writeTimeout(Duration.ofSeconds(300))
                .retryOnConnectionFailure(true)
                .build();
        log.info("OpenAiProvider initialized: baseUrl={}, models={}", this.baseUrl, this.supportedModels);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public List<String> getSupportedModels() {
        return supportedModels;
    }

    @Override
    public ChatResponse streamChat(ChatRequest request) {
        ensureModel(request);

        // 1. 构建流式请求体
        ObjectNode body = OpenAiChatRequestFactory.build(objectMapper, request);
        long timeoutMs = effectiveTimeoutMs(request);

        // 2. 构建带超时的 OkHttpClient（共享连接池，线程安全）
        OkHttpClient client = httpClient.newBuilder()
                .callTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .build();
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_MEDIA))
                .build();
        LlmStreamSink userSink = request.getStreamSink();

        // 3. 执行请求
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new LlmApiException("streamChat HTTP " + response.code() + ": " + err, response.code() >= 500, response.code());
            }
            ResponseBody rb = response.body();
            if (rb == null) {
                throw new LlmApiException("Empty stream body", true);
            }
            return parseSseStream(rb, userSink);
        } catch (LlmApiException e) {
            throw e;
        } catch (IOException e) {
            throw new LlmApiException("streamChat IO: " + e.getMessage(), e, true);
        }
    }

    @Override
    public ChatResponse chatSync(ChatRequest request) {
        ensureModel(request);

        // 1. 构建非流式请求体
        ObjectNode body = OpenAiChatRequestFactory.build(objectMapper, request);
        long timeoutMs = effectiveTimeoutMs(request);

        // 2. 构建带超时的 OkHttpClient（共享连接池，线程安全）
        OkHttpClient client = httpClient.newBuilder()
                .callTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .build();
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_MEDIA))
                .build();

        // 3. 执行请求
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new LlmApiException("chatSync HTTP " + response.code() + ": " + err, response.code() >= 500, response.code());
            }
            ResponseBody rb = response.body();
            if (rb == null) {
                throw new LlmApiException("Empty chatSync response", true);
            }
            JsonNode root = objectMapper.readTree(rb.string());
//            if (root.has("error")) {
//                String msg = root.path("error").path("message").asText(root.toString());
//                throw new LlmApiException("OpenAI error: " + msg, false);
//            }

//            log.info("OpenAiProvider responseRaw={}", JSONObject.toJSONString(root));
            ChatResponse chatResponse = mapCompletionJson(root);
//            LlmStreamSink sink = request.getStreamSink();
//            if (sink != null) {
//                if (built.getContent() != null && !built.getContent().isEmpty()) {
//                    sink.onTextDelta(built.getContent());
//                }
//                if (built.getUsageInputTokens() != null && built.getUsageOutputTokens() != null) {
//                    sink.onUsage(built.getUsageInputTokens(), built.getUsageOutputTokens());
//                }
//                sink.onMessageComplete(built.getFinishReason() != null ? built.getFinishReason() : "stop");
//            }

            log.info("OpenAiProvider chatResponse={}", JSONObject.toJSONString(chatResponse));
            return chatResponse;
        } catch (LlmApiException e) {
            throw e;
        } catch (IOException e) {
            throw new LlmApiException("chatSync IO error: " + e.getMessage(), e, true);
        }
    }

    private void ensureModel(ChatRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            if (defaultModel == null || defaultModel.isBlank()) {
                throw new LlmApiException("model is required (no defaultModel configured)", false);
            }
            request.setModel(defaultModel);
        }
    }

    private long effectiveTimeoutMs(ChatRequest request) {
        Long t = request.getTimeoutMs();
        return t != null && t > 0 ? t : DEFAULT_TIMEOUT_MS;
    }

    private ChatResponse mapCompletionJson(JsonNode root) {
        JsonNode choice0 = root.path("choices").path(0);
        JsonNode message = choice0.path("message");
        String content = extractMessageTextContent(message);
        String reasoning = message.path("reasoning_content").asText(null);
        if (reasoning != null && reasoning.isEmpty()) {
            reasoning = null;
        }
        List<ToolCall> tools = parseToolCallsFromMessage(message);
        TokenUsage tokenUsage = parseUsage(root.path("usage"));

        return ChatResponse.builder()
                .id(root.path("id").asText(null))
                .model(root.path("model").asText(null))
                .role(message.path("role").asText("assistant"))
                .content(content.isEmpty() ? null : content)
                .reasoningContent(reasoning)
                .toolCalls(tools.isEmpty() ? List.of() : tools)
                .finishReason(nullIfEmpty(choice0.path("finish_reason").asText(null)))
                .tokenUsage(tokenUsage)
                .rawJson(root)
                .build();
    }

    private TokenUsage parseUsage(JsonNode usage) {
        if (usage == null || usage.isNull()) {
            return null;
        }

        TokenUsage.TokenUsageBuilder builder = TokenUsage.builder();

        if (usage.has("prompt_tokens")) {
            builder.inputTokens(usage.get("prompt_tokens").asInt());
        }
        if (usage.has("completion_tokens")) {
            builder.completionTokens(usage.get("completion_tokens").asInt());
        }
        if (usage.has("total_tokens")) {
            builder.totalTokens(usage.get("total_tokens").asInt());
        }

        JsonNode promptDetails = usage.get("prompt_tokens_details");
        if (promptDetails != null && !promptDetails.isNull()) {
            TokenUsage.InputTokensDetails.InputTokensDetailsBuilder pdBuilder =
                    TokenUsage.InputTokensDetails.builder();
            if (promptDetails.has("cached_tokens")) {
                pdBuilder.cachedTokens(promptDetails.get("cached_tokens").asInt());
            }
            if (promptDetails.has("audio_tokens")) {
                pdBuilder.audioTokens(promptDetails.get("audio_tokens").asInt());
            }
//            if (promptDetails.has("text_tokens")) {
//                pdBuilder.textTokens(promptDetails.get("text_tokens").asInt());
//            }
            if (promptDetails.has("image_tokens")) {
                pdBuilder.imageTokens(promptDetails.get("image_tokens").asInt());
            }
            if (promptDetails.has("video_tokens")) {
                pdBuilder.videoTokens(promptDetails.get("video_tokens").asInt());
            }
            builder.inputTokensDetails(pdBuilder.build());
        }

        JsonNode completionDetails = usage.get("completion_tokens_details");
        if (completionDetails != null && !completionDetails.isNull()) {
            TokenUsage.CompletionTokensDetails.CompletionTokensDetailsBuilder cdBuilder =
                    TokenUsage.CompletionTokensDetails.builder();
            if (completionDetails.has("audio_tokens")) {
                cdBuilder.audioTokens(completionDetails.get("audio_tokens").asInt());
            }
            if (completionDetails.has("reasoning_tokens")) {
                cdBuilder.reasoningTokens(completionDetails.get("reasoning_tokens").asInt());
            }
            if (completionDetails.has("text_tokens")) {
                cdBuilder.textTokens(completionDetails.get("text_tokens").asInt());
            }
            builder.completionTokensDetails(cdBuilder.build());
        }

//        JsonNode cacheCreation = usage.get("cache_creation");
//        if (cacheCreation != null && !cacheCreation.isNull()) {
//            TokenUsage.CacheCreation.CacheCreationBuilder ccBuilder =
//                    TokenUsage.CacheCreation.builder();
//            if (cacheCreation.has("cache_creation_input_tokens")) {
//                ccBuilder.cacheCreationInputTokens(cacheCreation.get("cache_creation_input_tokens").asInt());
//            }
//            if (cacheCreation.has("cache_type")) {
//                ccBuilder.cacheType(cacheCreation.get("cache_type").asText(null));
//            }
//            builder.cacheCreation(ccBuilder.build());
//        }

        return builder.build();
    }

    private static String nullIfEmpty(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static String extractMessageTextContent(JsonNode message) {
        JsonNode c = message.get("content");
        if (c == null || c.isNull()) {
            return "";
        }
        if (c.isTextual()) {
            return c.asText();
        }
        if (c.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : c) {
                if ("text".equals(part.path("type").asText()) && part.has("text")) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(part.get("text").asText(""));
                }
            }
            return sb.toString();
        }
        return "";
    }

    private List<ToolCall> parseToolCallsFromMessage(JsonNode message) {
        JsonNode arr = message.get("tool_calls");
        if (arr == null || !arr.isArray()) {
            return List.of();
        }
        List<ToolCall> out = new ArrayList<>();
        for (JsonNode tc : arr) {
            JsonNode fn = tc.get("function");
            if (fn == null) {
                continue;
            }
            String id = tc.path("id").asText("");
            String name = fn.path("name").asText("");
            String argsRaw = fn.path("arguments").asText("{}");
            out.add(ToolCall.builder()
                    .toolCallId(id)
                    .name(name)
                    .argumentsRaw(argsRaw)
                    .arguments(parseArgumentsNode(argsRaw))
                    .build());
        }
        return out;
    }

    private JsonNode parseArgumentsNode(String argsRaw) {
        if (argsRaw == null || argsRaw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(argsRaw);
        } catch (Exception e) {
            return null;
        }
    }

    private ChatResponse parseSseStream(ResponseBody bodyStream, LlmStreamSink userSink) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream.byteStream(), StandardCharsets.UTF_8));
        StreamAgg agg = new StreamAgg();

        // 逐行解析 SSE 流
        String line;
        while ((line = reader.readLine()) != null) {
            log.info("OpenAiProvider streamChat line={}",line);
            if (line.isEmpty()) continue;

            if (!line.startsWith("data:")) {
                continue;
            }
            String payload = line.substring(5).trim();

            // 流式回复结束
            if ("data: [DONE]".equals(line)) {
                break ;
            }
//            if (payload.isEmpty() || "[DONE]".equals(payload)) {
//                if ("[DONE]".equals(payload)) {
//                    break;
//                }
//                continue;
//            }
            JsonNode chunk;
            try {
                chunk = objectMapper.readTree(payload);
            } catch (Exception e) {
                log.debug("Skip malformed SSE JSON: {}", e.getMessage());
                continue;
            }
            if (chunk.has("error")) {
                String msg = chunk.path("error").path("message").asText(chunk.toString());
                if (userSink != null) {
                    userSink.onError(msg, true);
                    userSink.onMessageComplete("error");
                }
                throw new LlmApiException("stream error: " + msg, false);
            }
            if (agg.id == null && chunk.has("id")) {
                agg.id = chunk.get("id").asText(null);
            }
            if (agg.model == null && chunk.has("model")) {
                agg.model = chunk.get("model").asText(null);
            }
            JsonNode choices = chunk.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode ch0 = choices.get(0);
                JsonNode delta = ch0.get("delta");
                if (delta != null) {
                    applyDelta(delta, agg, userSink);
                }
                String fr = ch0.path("finish_reason").asText(null);
                if (fr != null && !fr.isEmpty() && !"null".equals(fr)) {
                    agg.finishReason = fr;
                }
            }
            JsonNode usage = chunk.get("usage");
            if (usage != null) {
                TokenUsage tokenUsage = parseUsage(usage);
                agg.tokenUsage = tokenUsage;
//                agg.tokenUsage = UsageParser.parseUsage(usage);
//                if (agg.tokenUsage != null && userSink != null) {
//                    int inTok = agg.tokenUsage.getPromptTokens() != null ? agg.tokenUsage.getPromptTokens() : 0;
//                    int outTok = agg.tokenUsage.getCompletionTokens() != null ? agg.tokenUsage.getCompletionTokens() : 0;
//                    userSink.onUsage(inTok, outTok);
//                }
            }
        }
        List<ToolCall> toolCalls = agg.buildToolCalls(objectMapper);
        String contentStr = agg.text.length() > 0 ? agg.text.toString() : null;
        String reasoningStr = agg.reasoning.length() > 0 ? agg.reasoning.toString() : null;
        ChatResponse response = ChatResponse.builder()
                .id(agg.id)
                .model(agg.model)
                .role("assistant")
                .content(contentStr)
                .reasoningContent(reasoningStr)
                .toolCalls(toolCalls)
                .finishReason(agg.finishReason != null ? agg.finishReason : "stop")
                .tokenUsage(agg.tokenUsage)
                .build();
        if (userSink != null) {
            userSink.onMessageComplete(response.getFinishReason() != null ? response.getFinishReason() : "stop");
        }

        log.info("OpenAiProvider streamChat chatResponse={}", JSONObject.toJSONString(response));
        return response;
    }

    private void applyDelta(JsonNode delta, StreamAgg agg, LlmStreamSink userSink) {
        if (delta.has("content") && !delta.get("content").isNull()) {
            String t = delta.get("content").asText("");
            if (!t.isEmpty()) {
                agg.text.append(t);
                if (userSink != null) {
                    userSink.onTextDelta(t);
                }
            }
        }
        if (delta.has("reasoning_content") && delta.get("reasoning_content").isTextual()) {
            String r = delta.get("reasoning_content").asText("");
            if (!r.isEmpty()) {
                agg.reasoning.append(r);
                if (userSink != null) {
                    userSink.onReasoningDelta(r);
                }
            }
        }
        JsonNode tcalls = delta.get("tool_calls");
        if (tcalls == null || !tcalls.isArray()) {
            return;
        }
        for (JsonNode item : tcalls) {
            int index = item.path("index").asInt(-1);
            if (index < 0) {
                continue;
            }
            ToolSlot slot = agg.toolsByIndex.computeIfAbsent(index, k -> new ToolSlot());
            if (item.has("id") && !item.get("id").isNull()) {
                String id = item.get("id").asText("");
                if (!id.isEmpty()) {
                    slot.id = id;
                }
            }
            JsonNode fn = item.get("function");
            if (fn != null) {
                if (fn.has("name") && !fn.get("name").isNull()) {
                    String name = fn.get("name").asText("");
                    if (!name.isEmpty()) {
                        slot.name = name;
                    }
                }
                if (fn.has("arguments")) {
                    String frag = fn.get("arguments").asText("");
                    if (!frag.isEmpty()) {
                        slot.args.append(frag);
                        if (userSink != null && slot.id != null && !slot.id.isEmpty()) {
                            userSink.onToolCallArgumentsDelta(slot.id, frag);
                        }
                    }
                }
            }
            tryFireToolStart(slot, userSink);
        }
    }

    private static void tryFireToolStart(ToolSlot slot, LlmStreamSink userSink) {
        if (slot.started || userSink == null || slot.id == null || slot.id.isEmpty()
                || slot.name == null || slot.name.isEmpty()) {
            return;
        }
        userSink.onToolCallStart(slot.id, slot.name);
        slot.started = true;
    }

    private static final class StreamAgg {
        String id;
        String model;
        final StringBuilder text = new StringBuilder();
        final StringBuilder reasoning = new StringBuilder();
        final TreeMap<Integer, ToolSlot> toolsByIndex = new TreeMap<>();
        String finishReason;
        TokenUsage tokenUsage;

        List<ToolCall> buildToolCalls(ObjectMapper mapper) {
            List<ToolCall> list = new ArrayList<>();
            for (Map.Entry<Integer, ToolSlot> e : toolsByIndex.entrySet()) {
                ToolSlot s = e.getValue();
                if (s.id == null || s.id.isEmpty()) {
                    continue;
                }
                String raw = s.args.toString();
                JsonNode argsNode = null;
                if (!raw.isBlank()) {
                    try {
                        argsNode = mapper.readTree(raw);
                    } catch (Exception ignored) {
                    }
                }
                list.add(ToolCall.builder()
                        .toolCallId(s.id)
                        .name(s.name != null ? s.name : "")
                        .argumentsRaw(raw.isEmpty() ? "{}" : raw)
                        .arguments(argsNode)
                        .build());
            }
            return list;
        }
    }

    private static final class ToolSlot {
        String id;
        String name;
        final StringBuilder args = new StringBuilder();
        boolean started;
    }
}
