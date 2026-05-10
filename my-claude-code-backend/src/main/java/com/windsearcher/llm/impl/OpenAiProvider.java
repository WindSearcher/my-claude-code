package com.windsearcher.llm.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.windsearcher.domain.ChatRequest;
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

    private final String apiKey;
    private final String baseUrl;

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of();
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

        // 2. 转换消息列表 — Anthropic 内部格式 → OpenAI Chat Completions 格式
        for (Map<String, Object> msg : chatRequest.getMessages()) {
            String role = (String) msg.get("role");
            Object content = msg.get("content");

            if (content instanceof List<?> blocks) {
                // 检查是否包含 tool_result 块 → 转为 role:"tool" 消息
                boolean hasToolResult = blocks.stream().anyMatch(b ->
                        b instanceof Map<?,?> m && "tool_result".equals(m.get("type")));
                if (hasToolResult) {
                    for (Object block : blocks) {
                        if (block instanceof Map<?,?> b && "tool_result".equals(b.get("type"))) {
                            ObjectNode toolMsg = messagesArray.addObject();
                            toolMsg.put("role", "tool");
                            toolMsg.put("tool_call_id", (String) b.get("tool_use_id"));
                            Object resultContent = b.get("content");
                            toolMsg.put("content", resultContent != null ? resultContent.toString() : "");
                        }
                    }
                    continue;
                }

                // 检查 assistant 消息是否包含 tool_use 块 → 转为 tool_calls 数组
                boolean hasToolUse = blocks.stream().anyMatch(b ->
                        b instanceof Map<?,?> m && "tool_use".equals(m.get("type")));
                if ("assistant".equals(role) && hasToolUse) {
                    ObjectNode msgNode = messagesArray.addObject();
                    msgNode.put("role", "assistant");
                    // 提取文本内容
                    StringBuilder textContent = new StringBuilder();
                    StringBuilder thinkingContent = new StringBuilder();
                    for (Object block : blocks) {
                        if (block instanceof Map<?,?> b) {
                            if ("text".equals(b.get("type"))) {
                                Object text = b.get("text");
                                if (text != null && !text.toString().isEmpty()) {
                                    if (!textContent.isEmpty()) textContent.append("\n");
                                    textContent.append(text);
                                }
                            } else if ("thinking".equals(b.get("type"))) {
                                Object thinking = b.get("thinking");
                                if (thinking != null && !thinking.toString().isEmpty()) {
                                    thinkingContent.append(thinking);
                                }
                            }
                        }
                    }
                    if (!textContent.isEmpty()) {
                        msgNode.put("content", textContent.toString());
                    } else {
                        msgNode.putNull("content");
                    }
                    // DeepSeek: reasoning_content 必须回传
                    if (!thinkingContent.isEmpty()) {
                        msgNode.put("reasoning_content", thinkingContent.toString());
                    }
                    // 构建 tool_calls 数组
                    ArrayNode toolCalls = msgNode.putArray("tool_calls");
                    for (Object block : blocks) {
                        if (block instanceof Map<?,?> b && "tool_use".equals(b.get("type"))) {
                            ObjectNode tc = toolCalls.addObject();
                            tc.put("id", (String) b.get("id"));
                            tc.put("type", "function");
                            ObjectNode fn = tc.putObject("function");
                            fn.put("name", (String) b.get("name"));
                            Object input = b.get("input");
                            try {
                                fn.put("arguments", input != null
                                        ? objectMapper.writeValueAsString(input) : "{}");
                            } catch (Exception e) {
                                fn.put("arguments", "{}");
                            }
                        }
                    }
                    continue;
                }

                // 普通 assistant/user 消息: 提取文本内容为字符串
                StringBuilder textContent = new StringBuilder();
                StringBuilder thinkingContent = new StringBuilder();
                for (Object block : blocks) {
                    if (block instanceof Map<?,?> b) {
                        if ("text".equals(b.get("type"))) {
                            Object text = b.get("text");
                            if (text != null && !text.toString().isEmpty()) {
                                if (!textContent.isEmpty()) textContent.append("\n");
                                textContent.append(text);
                            }
                        } else if ("thinking".equals(b.get("type")) && "assistant".equals(role)) {
                            Object thinking = b.get("thinking");
                            if (thinking != null && !thinking.toString().isEmpty()) {
                                thinkingContent.append(thinking);
                            }
                        }
                    }
                }
                ObjectNode msgNode = messagesArray.addObject();
                msgNode.put("role", role != null ? role : "user");
                msgNode.put("content", textContent.toString());
                // DeepSeek: reasoning_content 必须回传
                if ("assistant".equals(role) && !thinkingContent.isEmpty()) {
                    msgNode.put("reasoning_content", thinkingContent.toString());
                }
            } else {
                // 纯字符串消息
                ObjectNode msgNode = messagesArray.addObject();
                msgNode.put("role", role != null ? role : "user");
                if (content instanceof String s) {
                    msgNode.put("content", s);
                } else {
                    msgNode.put("content", content != null ? content.toString() : "");
                }
            }
        }

        // 3. 工具定义
        if (CollectionUtils.isEmpty(chatRequest.getTools())) {
            ArrayNode toolsArray = root.putArray("tools");
            for (Map<String, Object> tool : tools) {
                toolsArray.add(objectMapper.valueToTree(tool));
            }
        }

        return root;
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

        // 4. 执行请求（429 时一次指数退避重试）
        // 429-表示模型限流；
        for (int attempt = 0; attempt < 2; attempt++) {
            try (Response response = syncClient.newCall(request).execute()) {
                if (response.code() == 429 && attempt == 0) {
                    long waitMs = 1000;
                    String retryAfter = response.header("Retry-After");
                    if (retryAfter != null) {
                        try { waitMs = Long.parseLong(retryAfter) * 1000; }
                        catch (NumberFormatException ignored) {}
                    }
                    log.warn("chatSync 429 rate limited, retrying after {}ms", Math.min(waitMs, 5000));
                    Thread.sleep(Math.min(waitMs, 5000));
                    continue;
                }

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


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmApiException("chatSync interrupted", false);
            } catch (LlmApiException e) {
                throw e;
            } catch (IOException e) {
                throw new LlmApiException("chatSync IO error: " + e.getMessage(), true);
            }
        }
        throw new LlmApiException("chatSync failed after 429 retry", true, 429);
    }
}
