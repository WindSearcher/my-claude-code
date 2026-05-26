package com.windsearcher.llm.protocol.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.windsearcher.domain.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 构建 OpenAI Chat Completions 兼容请求体（含工具、多模态 user 内容、assistant tool_calls / reasoning_content）。
 *
 */
public final class OpenAiChatRequestFactory {

    private OpenAiChatRequestFactory() {
    }

    public static ObjectNode build(ObjectMapper mapper, ChatRequest request) {
        // 1.模型名称
        ObjectNode root = mapper.createObjectNode();
        root.put("model", request.getModel());
        if (request.getMaxTokens() != null) {
            root.put("max_tokens", request.getMaxTokens());
        }

        // 2.是否流式输出
        root.put("stream", request.isStream());
        if (request.isStream()) {
            ObjectNode streamOpts = mapper.createObjectNode();
            streamOpts.put("include_usage", true);
            root.set("stream_options", streamOpts);
        }

        // 3.温度系数
        if (request.getTemperature() != null) {
            root.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            root.put("top_p", request.getTopP());
        }

        // 4.转换消息列表 — Anthropic 内部格式 → OpenAI Chat Completions 格式
        ArrayNode messages = root.putArray("messages");
        if (StringUtils.hasText(request.getSystemPrompt())) {
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", request.getSystemPrompt());
        }

        List<ChatMessage> list = request.getMessages() == null ? List.of() : request.getMessages();
        for (ChatMessage m : list) {
            if (m.getRole() == Role.USER) {
                ObjectNode sys = messages.addObject();
                sys.put("role", m.getRole().getType());
                sys.put("content", m.getContent());
                continue;
            }
            if (m.getRole() == Role.ASSISTANT) {
                JsonNode asst = assistantMessage(mapper, m);
                if (asst != null) {
                    messages.add(asst);
                }
                continue;
            }
            if (m.getRole() == Role.TOOL) {
                messages.add(toolMessage(mapper, m));
                continue;
            }
            messages.add(userMessage(mapper, m));
        }

        if (!CollectionUtils.isEmpty(request.getTools())) {
            ArrayNode tools = root.putArray("tools");
            for (Map<String, Object> tool : request.getTools()) {
                tools.add(normalizeOpenAiTool(mapper, tool));
            }
            JsonNode tc = toolChoiceValue(mapper, request.getToolChoice());
            if (tc != null) {
                root.set("tool_choice", tc);
            }
        }

        if (!CollectionUtils.isEmpty(request.getStopSequences())) {
            ArrayNode stop = root.putArray("stop");
            for (String s : request.getStopSequences()) {
                stop.add(s);
            }
        }

        return root;
    }

    private static ObjectNode userMessage(ObjectMapper mapper, ChatMessage m) {
        ObjectNode o = mapper.createObjectNode();
        o.put("role", "user");
        o.put("content", m.getContent() != null ? m.getContent() : "");
//        if (CollectionUtils.isEmpty(m.getBlocks())) {
//            o.put("content", m.getContent() != null ? m.getContent() : "");
//            return o;
//        }
//        boolean multimodal = m.getBlocks().stream().anyMatch(b -> "image".equals(b.getType()));
//        if (!multimodal) {
//            StringBuilder text = new StringBuilder();
//            for (ContentBlock b : m.getBlocks()) {
//                if ("text".equals(b.getType()) && b.getText() != null) {
//                    if (!text.isEmpty()) {
//                        text.append('\n');
//                    }
//                    text.append(b.getText());
//                }
//            }
//            o.put("content", text.isEmpty() && m.getContent() != null ? m.getContent() : text.toString());
//            return o;
//        }
//        ArrayNode parts = mapper.createArrayNode();
//        for (ContentBlock b : m.getBlocks()) {
//            if ("text".equals(b.getType()) && StringUtils.hasText(b.getText())) {
//                ObjectNode t = mapper.createObjectNode();
//                t.put("type", "text");
//                t.put("text", b.getText());
//                parts.add(t);
//            } else if ("image".equals(b.getType()) && StringUtils.hasText(b.getImageUrl())) {
//                ObjectNode img = mapper.createObjectNode();
//                img.put("type", "image_url");
//                ObjectNode url = mapper.createObjectNode();
//                url.put("url", b.getImageUrl());
//                img.set("image_url", url);
//                parts.add(img);
//            }
//        }
//        if (parts.isEmpty() && m.getContent() != null) {
//            ObjectNode t = mapper.createObjectNode();
//            t.put("type", "text");
//            t.put("text", m.getContent());
//            parts.add(t);
//        }
//        o.set("content", parts);
        return o;
    }

    private static JsonNode assistantMessage(ObjectMapper mapper, ChatMessage m) {
        if (CollectionUtils.isEmpty(m.getToolCalls())) {
            ObjectNode o = mapper.createObjectNode();
            o.put("role", "assistant");
            o.put("content", m.getContent() != null ? m.getContent() : "");
            return o;
        }
        boolean hasToolUse = m.getToolCalls() != null && m.getToolCalls().size() > 0;
//        StringBuilder text = new StringBuilder();
//        StringBuilder thinking = new StringBuilder();
//
//        text.append(m.getContent());
//        thinking.append(m.getReasoningContent());
//
//        for (ContentBlock b : m.getBlocks()) {
//            if ("text".equals(b.getType()) && b.getText() != null) {
//                if (!text.isEmpty()) {
//                    text.append('\n');
//                }
//                text.append(b.getText());
//            } else if ("thinking".equals(b.getType()) && b.getText() != null) {
//                thinking.append(b.getText());
//            }
//        }

        ObjectNode o = mapper.createObjectNode();
        o.put("role", "assistant");
        if (hasToolUse) {
            o.put("content", m.getContent() != null ? m.getContent() : "");
            o.put("reasoning_content", m.getReasoningContent() != null ? m.getReasoningContent() : "");

            ArrayNode toolCalls = o.putArray("tool_calls");
            for (ToolCall b : m.getToolCalls()) {
                ObjectNode tc = toolCalls.addObject();
                tc.put("id", b.getToolCallId() != null ? b.getToolCallId() : "");
                tc.put("type", "function");
                ObjectNode fn = tc.putObject("function");
                fn.put("name", b.getToolName() != null ? b.getToolName() : "");
//                String args = "{}";
//                if (StringUtils.hasText(b.getToolInput())) {
//                    args = b.getToolInput();
//                }
                fn.put("arguments", b.getArgumentsRaw());
            }
        } else {
            o.put("content", m.getContent() != null ? m.getContent() : "");
            o.put("reasoning_content", m.getReasoningContent() != null ? m.getReasoningContent() : "");
        }
        return o;
    }

    private static ObjectNode toolMessage(ObjectMapper mapper, ChatMessage m) {
        ObjectNode o = mapper.createObjectNode();
        o.put("role", "tool");
        o.put("tool_call_id", m.getToolCallId() != null ? m.getToolCallId() : "");
        String content = m.getToolResult() != null ? m.getToolResult() : "";
//        if (!CollectionUtils.isEmpty(m.getBlocks())) {
//            ContentBlock b = m.getBlocks().get(0);
//            if (b.getToolResult() != null) {
//                content = b.getToolResult();
//            }
//        }
        o.put("content", content);
//        if (StringUtils.hasText(m.getToolName())) {
//            o.put("name", m.getToolName());
//        }
        return o;
    }

    private static ObjectNode normalizeOpenAiTool(ObjectMapper mapper, Map<String, Object> tool) {
        if (tool != null && "function".equals(String.valueOf(tool.get("type"))) && tool.get("function") instanceof Map<?, ?>) {
            ObjectNode wrap = mapper.createObjectNode();
            wrap.put("type", "function");
            wrap.set("function", mapper.valueToTree(tool.get("function")));
            return wrap;
        }
        ObjectNode fn = mapper.createObjectNode();
        Object name = tool != null ? tool.get("name") : "tool";
        fn.put("name", name != null ? name.toString() : "tool");
        if (tool != null && tool.get("description") != null) {
            fn.put("description", tool.get("description").toString());
        }
        JsonNode params = parseSchema(mapper, tool != null ? tool.get("inputSchema") : null);
        fn.set("parameters", params != null ? params : mapper.createObjectNode().put("type", "object"));

        ObjectNode wrap = mapper.createObjectNode();
        wrap.put("type", "function");
        wrap.set("function", fn);
        return wrap;
    }

    private static JsonNode parseSchema(ObjectMapper mapper, Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof JsonNode j) {
            return j;
        }
        if (raw instanceof Map<?, ?> m) {
            return mapper.valueToTree(m);
        }
        if (raw instanceof String s) {
            try {
                return mapper.readTree(s);
            } catch (Exception e) {
                return mapper.createObjectNode().put("type", "object");
            }
        }
        return mapper.valueToTree(raw);
    }

    private static JsonNode toolChoiceValue(ObjectMapper mapper, String toolChoice) {
        if (!StringUtils.hasText(toolChoice) || "auto".equalsIgnoreCase(toolChoice)) {
            return null;
        }
        if ("none".equalsIgnoreCase(toolChoice)) {
            return TextNode.valueOf("none");
        }
        if ("any".equalsIgnoreCase(toolChoice) || "required".equalsIgnoreCase(toolChoice)) {
            return TextNode.valueOf("required");
        }
        ObjectNode o = mapper.createObjectNode();
        o.put("type", "function");
        ObjectNode fn = o.putObject("function");
        fn.put("name", toolChoice);
        return o;
    }
}
