package com.windsearcher.llm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.windsearcher.domain.ChatRequest;
import com.windsearcher.domain.ChatResponse;
import com.windsearcher.domain.TokenUsage;
import com.windsearcher.domain.ToolCall;
import com.windsearcher.llm.util.UsageParser;
import com.windsearcher.exception.LlmApiException;
import com.windsearcher.llm.LlmProvider;
import com.windsearcher.llm.LlmStreamSink;
import com.windsearcher.llm.protocol.anthropic.AnthropicMessageRequestFactory;
import com.windsearcher.llm.protocol.anthropic.AnthropicMessagesClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API 供应商实现 — 委托 {@link AnthropicMessagesClient} /
 * {@link AnthropicMessageRequestFactory}，与 {@code services/api/client.ts}、
 * {@code utils/sideQuery.ts} 的调用语义对齐。
 */
@Slf4j
public class AnthropicProvider implements LlmProvider {

    private static final long DEFAULT_TIMEOUT_MS = 600_000L;

    private final String providerName;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final List<String> supportedModels;
    private final AnthropicMessagesClient client;

    public AnthropicProvider(
            String providerName,
            ObjectMapper objectMapper,
            String apiKey,
            String baseUrl,
            String defaultModel,
            List<String> supportedModels,
            OkHttpClient httpClient) {
        this.providerName = providerName;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://api.anthropic.com"
                : (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        this.defaultModel = defaultModel;
        this.supportedModels = supportedModels == null ? List.of() : List.copyOf(supportedModels);
        this.client = new AnthropicMessagesClient(objectMapper, httpClient);
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
        ObjectNode body = AnthropicMessageRequestFactory.build(objectMapper, request, true);
        long timeoutMs = request.getTimeoutMs() != null ? request.getTimeoutMs() : DEFAULT_TIMEOUT_MS;
        StreamAggregateSink agg = new StreamAggregateSink(objectMapper, request.getStreamSink());
        client.createStreaming(baseUrl, apiKey, body, timeoutMs, agg);
        return agg.toResponse(request.getModel());
    }

    @Override
    public ChatResponse chatSync(ChatRequest request) {
        ensureModel(request);
        ObjectNode body = AnthropicMessageRequestFactory.build(objectMapper, request, false);
        long timeoutMs = request.getTimeoutMs() != null ? request.getTimeoutMs() : DEFAULT_TIMEOUT_MS;
        try {
            JsonNode root = client.createBlocking(baseUrl, apiKey, body, timeoutMs);
            ChatResponse response = mapBlockingToResponse(root, request.getModel());
            LlmStreamSink sink = request.getStreamSink();
//            if (sink != null) {
//                if (response.getContent() != null && !response.getContent().isEmpty()) {
//                    sink.onTextDelta(response.getContent());
//                }
//                if (response.getUsageInputTokens() != null && response.getUsageOutputTokens() != null) {
//                    sink.onUsage(response.getUsageInputTokens(), response.getUsageOutputTokens());
//                }
//                String stop = response.getFinishReason() != null ? response.getFinishReason() : "end_turn";
//                sink.onMessageComplete(stop);
//            }
            return response;
        } catch (IOException e) {
            throw new LlmApiException("Anthropic chatSync failed: " + e.getMessage(), e, true);
        }
    }

    private ChatResponse mapBlockingToResponse(JsonNode root, String modelFallback) {
        String text = extractAssistantText(root);
        String thinking = extractThinkingText(root);
        List<ToolCall> tools = extractToolUses(root);
        TokenUsage tokenUsage = UsageParser.parseUsage(root.path("usage"));
        return ChatResponse.builder()
                .id(root.path("id").asText(null))
                .model(root.path("model").asText(modelFallback))
                .role("assistant")
                .content(text.isEmpty() ? null : text)
                .reasoningContent(thinking.isEmpty() ? null : thinking)
                .toolCalls(tools)
                .finishReason(mapStopReason(root.path("stop_reason").asText(null)))
                .tokenUsage(tokenUsage)
                .rawJson(root)
                .build();
    }

    private static List<ToolCall> extractToolUses(JsonNode root) {
        JsonNode content = root.get("content");
        if (content == null || !content.isArray()) {
            return List.of();
        }
        List<ToolCall> list = new ArrayList<>();
        for (JsonNode block : content) {
            if (!"tool_use".equals(block.path("type").asText())) {
                continue;
            }
            JsonNode input = block.get("input");
            String raw = input != null ? input.toString() : "{}";
            list.add(ToolCall.builder()
                    .toolCallId(block.path("id").asText(""))
                    .name(block.path("name").asText(""))
                    .arguments(input)
                    .argumentsRaw(raw)
                    .build());
        }
        return list;
    }

    private static String extractThinkingText(JsonNode root) {
        JsonNode content = root.get("content");
        if (content == null || !content.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : content) {
            if (!"thinking".equals(block.path("type").asText())) {
                continue;
            }
            String t = block.path("thinking").asText("");
            if (t.isEmpty()) {
                t = block.path("text").asText("");
            }
            if (!t.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(t);
            }
        }
        return sb.toString();
    }

    private void ensureModel(ChatRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            if (defaultModel == null || defaultModel.isBlank()) {
                throw new LlmApiException("model is required (no defaultModel configured)", false);
            }
            request.setModel(defaultModel);
        }
    }

    private static String mapStopReason(String api) {
        if (api == null) {
            return "end_turn";
        }
        return switch (api) {
            case "max_tokens" -> "max_tokens";
            case "tool_use" -> "tool_use";
            case "stop_sequence" -> "stop_sequence";
            case "end_turn" -> "end_turn";
            default -> "end_turn";
        };
    }

    private static String extractAssistantText(JsonNode root) {
        JsonNode content = root.get("content");
        if (content == null || !content.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                String t = block.path("text").asText("");
                if (!t.isEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(t);
                }
            }
        }
        return sb.toString();
    }

    private static final class StreamAggregateSink implements LlmStreamSink {

        private final ObjectMapper mapper;
        private final LlmStreamSink delegate;
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();
        private final LinkedHashMap<String, ToolSlot> tools = new LinkedHashMap<>();
        private String stopReason = "end_turn";
        private int usageIn = -1;
        private int usageOut = -1;

        StreamAggregateSink(ObjectMapper mapper, LlmStreamSink delegate) {
            this.mapper = mapper;
            this.delegate = delegate;
        }

        @Override
        public void onTextDelta(String fragment) {
            if (fragment != null && !fragment.isEmpty()) {
                text.append(fragment);
            }
            if (delegate != null) {
                delegate.onTextDelta(fragment);
            }
        }

        @Override
        public void onReasoningDelta(String fragment) {
            if (fragment != null && !fragment.isEmpty()) {
                reasoning.append(fragment);
            }
            if (delegate != null) {
                delegate.onReasoningDelta(fragment);
            }
        }

        @Override
        public void onToolCallStart(String id, String name) {
            if (id != null && !id.isEmpty()) {
                tools.computeIfAbsent(id, k -> new ToolSlot(k)).name = name != null ? name : "";
            }
            if (delegate != null) {
                delegate.onToolCallStart(id, name);
            }
        }

        @Override
        public void onToolCallArgumentsDelta(String id, String jsonFragment) {
            if (id != null && !id.isEmpty() && jsonFragment != null && !jsonFragment.isEmpty()) {
                tools.computeIfAbsent(id, k -> new ToolSlot(k)).args.append(jsonFragment);
            }
            if (delegate != null) {
                delegate.onToolCallArgumentsDelta(id, jsonFragment);
            }
        }

        @Override
        public void onUsage(int inputTokens, int outputTokens) {
            usageIn = inputTokens;
            usageOut = outputTokens;
            if (delegate != null) {
                delegate.onUsage(inputTokens, outputTokens);
            }
        }

        @Override
        public void onMessageComplete(String stopReason) {
            if (stopReason != null && !stopReason.isEmpty()) {
                this.stopReason = stopReason;
            }
            if (delegate != null) {
                delegate.onMessageComplete(stopReason);
            }
        }

        @Override
        public void onError(String message, boolean retryable) {
            if (delegate != null) {
                delegate.onError(message, retryable);
            }
        }

        ChatResponse toResponse(String model) {
            List<ToolCall> toolCalls = new ArrayList<>();
            for (Map.Entry<String, ToolSlot> e : tools.entrySet()) {
                ToolSlot s = e.getValue();
                String raw = s.args.toString();
                JsonNode argsNode = null;
                if (!raw.isBlank()) {
                    try {
                        argsNode = mapper.readTree(raw);
                    } catch (Exception ignored) {
                    }
                }
                toolCalls.add(ToolCall.builder()
                        .toolCallId(s.id)
                        .name(s.name != null ? s.name : "")
                        .argumentsRaw(raw.isEmpty() ? "{}" : raw)
                        .arguments(argsNode)
                        .build());
            }
            TokenUsage tokenUsage = null;
//            if (usageIn >= 0 || usageOut >= 0) {
//                tokenUsage = TokenUsage.builder()
//                        .promptTokens(usageIn >= 0 ? usageIn : null)
//                        .completionTokens(usageOut >= 0 ? usageOut : null)
//                        .build();
//            }
            return ChatResponse.builder()
                    .model(model)
                    .role("assistant")
                    .content(text.isEmpty() ? null : text.toString())
                    .reasoningContent(reasoning.isEmpty() ? null : reasoning.toString())
                    .toolCalls(toolCalls)
                    .finishReason(stopReason)
                    .tokenUsage(tokenUsage)
                    .build();
        }
    }

    private static final class ToolSlot {
        final String id;
        String name = "";
        final StringBuilder args = new StringBuilder();

        ToolSlot(String id) {
            this.id = id;
        }
    }
}
