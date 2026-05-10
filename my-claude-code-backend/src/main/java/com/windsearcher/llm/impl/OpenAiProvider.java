package com.windsearcher.llm.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.windsearcher.domain.ChatMessage;
import com.windsearcher.domain.ChatRequest;
import com.windsearcher.domain.Role;
import com.windsearcher.domain.ToolCall;
import com.windsearcher.exception.LlmApiException;
import com.windsearcher.llm.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAi协议层，支持所有 OpenAI Chat Completions API 兼容的模型服务。
 *  * <p>
 *  * 通过 baseUrl 可配置性，一套代码同时支持:
 *  * <ul>
 *  *   <li>OpenAI 官方 (https://api.openai.com/v1)</li>
 *  *   <li>Ollama 本地模型 (http://localhost:11434/v1)</li>
 *  *   <li>通义千问 DashScope (https://dashscope.aliyuncs.com/compatible-mode/v1)</li>
 *  *   <li>其他 OpenAI 兼容 API（DeepSeek、Moonshot、智谱等）</li>
 *  * </ul>
 *  *
 *  流式如何返回？
 */
@Slf4j
public class OpenAiProvider implements LlmProvider {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json");

    private final ObjectMapper objectMapper;

    private final OkHttpClient httpClient;
    private final String providerName;
    private final List<String> supportedModels;
    private final String apiKey;
    private final String baseUrl;

    /** 内置模型能力映射表*/
//    private static final Map<String, ModelCapabilities> MODEL_CAPABILITIES = Map.ofEntries(
//            // OpenAI 模型
//            Map.entry("gpt-4o", new ModelCapabilities("gpt-4o", "GPT-4o", 16384, 128000, true, false, true, true, 0.005, 0.015)),
//            Map.entry("gpt-4o-mini", new ModelCapabilities("gpt-4o-mini", "GPT-4o Mini", 16384, 128000, true, false, true, true, 0.00015, 0.0006)),
//            Map.entry("gpt-4-turbo", new ModelCapabilities("gpt-4-turbo", "GPT-4 Turbo", 4096, 128000, true, false, true, true, 0.01, 0.03)),
//            // DeepSeek 模型
//            Map.entry("deepseek-chat", new ModelCapabilities("deepseek-chat", "DeepSeek Chat", 8192, 64000, true, true, false, true, 0.00027, 0.0011)),
//            Map.entry("deepseek-reasoner", new ModelCapabilities("deepseek-reasoner", "DeepSeek Reasoner", 8192, 64000, true, true, false, false, 0.00055, 0.0022)),
//            Map.entry("deepseek-v4-pro", new ModelCapabilities("deepseek-v4-pro", "DeepSeek V4 Pro", 384000, 1000000, true, true, false, true, 0.001, 0.004)),
//            Map.entry("deepseek-v4-flash", new ModelCapabilities("deepseek-v4-flash", "DeepSeek V4 Flash", 384000, 1000000, true, true, false, true, 0.0005, 0.002)),
//            // 阿里云百炼 - 通义千问模型（qwen-max/plus/turbo/3.6-plus 已迁移至 ModelRegistry.BUILTIN_MODELS）
//            Map.entry("qwen-coder-plus", new ModelCapabilities("qwen-coder-plus", "通义千问 Coder Plus", 8192, 131072, true, false, false, true, 0.0007, 0.002))
//    );

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

        this.supportedModels = supportedModels;

        this.httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(
                        20,
                       20,
                        java.util.concurrent.TimeUnit.SECONDS))
                .connectTimeout(Duration.ofSeconds(300))
                .readTimeout(Duration.ofMinutes(5)) // SSE 5分钟读超时（防止连接泄漏）
                .writeTimeout(Duration.ofSeconds(300))
                .retryOnConnectionFailure(true)
                .build();

        log.info("OpenAIprovider initialized: baseUrl={}, models={}", this.baseUrl, supportedModels);
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
    public void streamChat(ChatRequest request) {

    }


    /**
     * 构建基础请求体（不含 stream 设置）。
     * 公共逻辑：model、max_tokens、messages、system prompt、tools。
     * 由 buildOpenAiRequest（流式）和 chatSync（非流式）共享。
     */
    private ObjectNode buildBaseRequest(ChatRequest chatRequest) {

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", chatRequest.getModel());
        root.put("max_tokens", chatRequest.getMaxTokens());

        ArrayNode messagesArray = root.putArray("messages");

        // 1. 系统提示 → system 消息
        if (StringUtils.isNotEmpty(chatRequest.getSystemPrompt())) {
            ObjectNode sysMsg = messagesArray.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", chatRequest.getSystemPrompt());
        }

        if (chatRequest.getTemperature() != null) {
            root.put("temperature", chatRequest.getTemperature());
        }

        if (chatRequest.getTopP() != null) {
            root.put("top_p", chatRequest.getTopP());
        }

        root.put("stream", chatRequest.isStream());

        // 2. 转换消息列表 — Anthropic 内部格式 → OpenAI Chat Completions 格式
        ArrayNode messages = buildOpenAiMessage(chatRequest.getMessages());
        root.set("messages", messages);


        // 3. 工具定义
        if (CollectionUtils.isEmpty(chatRequest.getTools())) {
            ArrayNode toolsArray = root.putArray("tools");
            for (Map<String, Object> tool : chatRequest.getTools()) {
                toolsArray.add(objectMapper.valueToTree(tool));
            }
        }

        return root;
    }

    private ArrayNode buildOpenAiMessage(List<ChatMessage> chatMessages) {
        ArrayNode messages = objectMapper.createArrayNode();
        for (ChatMessage chatMessage : chatMessages) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("role", chatMessage.getRole().getDesc());
            node.put("content", chatMessage.getContent());
            // tool result message
//            if (chatMessage.getRole() == Role.TOOL) {
//                if (chatMessage.getToolCallId() != null) {
//                    node.put("tool_call_id", chatMessage.getToolCallId());
//                }
//                if (chatMessage.getToolName() != null) {
//
//                }
//            }

            // assistant含工具调用
            boolean hasToolCall = chatMessage.getToolCalls() != null
                    && !chatMessage.getToolCalls().isEmpty();

            if (chatMessage.getRole() == Role.ASSISTANT && hasToolCall) {
                ArrayNode arr = objectMapper.createArrayNode();
                for (ToolCall toolCall : chatMessage.getToolCalls()) {
                    ObjectNode tcn = objectMapper.createObjectNode();
                    tcn.put("id", toolCall.getId());
                    tcn.put("type", "function");
                    ObjectNode fn = objectMapper.createObjectNode();

                    fn.put("name", toolCall.getName());
                    String arguments;
                    if (toolCall.getArgumentsRaw() != null) {
                        arguments = toolCall.getArgumentsRaw();
                    } else if (toolCall.getArguments() != null) {
                        arguments = toolCall.getArguments().toString();
                    } else {
                        arguments = "{}";
                    }
                    fn.put("arguments", arguments);
                    tcn.set("function", tcn);
                    arr.add(tcn);
                }

                node.set("tool_calls", arr);
            }

            messages.add(node);
        }

        return messages;
    }


    @Override
    public void chatSync(ChatRequest chatRequest) {

        // 1. 构建非流式请求体
        ObjectNode requestBody = buildBaseRequest(chatRequest);
        requestBody.put("stream", false);

        // 2. 添加 stop sequences
        if (chatRequest.getStopSequences() != null && chatRequest.getStopSequences().size() > 0) {
            ArrayNode stopArray = requestBody.putArray("stop");
            for (String seq : chatRequest.getStopSequences()) {
                stopArray.add(seq);
            }
        }

        // 3. 构建带超时的 OkHttpClient（共享连接池，线程安全）
        OkHttpClient syncClient = httpClient.newBuilder()
                .callTimeout(Duration.ofMillis(chatRequest.getTimeoutMs()))
                .readTimeout(Duration.ofMillis(chatRequest.getTimeoutMs()))
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA))
                .build();

        try (Response response = syncClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new LlmApiException(
                        "chatSync HTTP " + response.code(),
                        response.code() >= 500, response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new LlmApiException("Empty chatSync response", true);
            }

            JsonNode root = objectMapper.readTree(body.string());

            // TODO.帮我将返回结果写入到ChatResponse中

        } catch (LlmApiException e) {
            throw e;
        } catch (IOException e) {
            throw new LlmApiException("chatSync IO error: " + e.getMessage(), true);
        }

    }
}
