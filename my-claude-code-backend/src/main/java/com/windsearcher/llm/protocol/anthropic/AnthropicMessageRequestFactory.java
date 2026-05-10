package com.windsearcher.llm.protocol.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.windsearcher.domain.ChatMessage;
import com.windsearcher.domain.ChatRequest;
import com.windsearcher.domain.ContentBlock;
import com.windsearcher.domain.Role;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 构建 Anthropic Messages API 请求体，语义对齐
 * {@code @anthropic-ai/sdk} 的 {@code messages.create} 参数（model / system / messages / tools / tool_choice / stream）。
 */
public final class AnthropicMessageRequestFactory {

    private static final int DEFAULT_MAX_TOKENS = 8192;

    private AnthropicMessageRequestFactory() {
    }

    public static ObjectNode build(ObjectMapper mapper, ChatRequest request, boolean stream) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", request.getModel());
        int max = request.getMaxTokens() != null ? request.getMaxTokens() : DEFAULT_MAX_TOKENS;
        root.put("max_tokens", max);
        root.put("stream", stream);

        String system = mergeSystemPrompt(request);
        if (StringUtils.hasText(system)) {
            root.put("system", system);
        }

        ArrayNode messages = root.putArray("messages");
        for (ChatMessage m : nullSafe(request.getMessages())) {
            JsonNode node = toAnthropicMessage(mapper, m);
            if (node != null) {
                messages.add(node);
            }
        }

        if (!CollectionUtils.isEmpty(request.getTools())) {
            ArrayNode tools = root.putArray("tools");
            for (Map<String, Object> tool : request.getTools()) {
                tools.add(normalizeTool(mapper, tool));
            }
            ObjectNode toolChoice = toolChoiceNode(mapper, request.getToolChoice());
            if (toolChoice != null) {
                root.set("tool_choice", toolChoice);
            }
        }

        if (!CollectionUtils.isEmpty(request.getStopSequences())) {
            ArrayNode stop = root.putArray("stop_sequences");
            for (String s : request.getStopSequences()) {
                stop.add(s);
            }
        }

        if (request.getTemperature() != null) {
            root.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            root.put("top_p", request.getTopP());
        }

        if (!CollectionUtils.isEmpty(request.getMetadata())) {
            root.set("metadata", mapper.valueToTree(request.getMetadata()));
        }

        return root;
    }

    private static List<ChatMessage> nullSafe(List<ChatMessage> messages) {
        return messages == null ? List.of() : messages;
    }

    private static String mergeSystemPrompt(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            sb.append(request.getSystemPrompt().trim());
        }
        for (ChatMessage m : nullSafe(request.getMessages())) {
            if (m.getRole() == Role.SYSTEM && StringUtils.hasText(m.getContent())) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(m.getContent().trim());
            }
        }
        return sb.toString();
    }

    private static JsonNode toAnthropicMessage(ObjectMapper mapper, ChatMessage m) {
        if (m.getRole() == Role.SYSTEM) {
            return null;
        }
        if (m.getRole() == Role.TOOL) {
            return toolResultMessage(mapper, m);
        }
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", m.getRole() == Role.ASSISTANT ? "assistant" : "user");

        if (!CollectionUtils.isEmpty(m.getBlocks())) {
            ArrayNode arr = mapper.createArrayNode();
            for (ContentBlock b : m.getBlocks()) {
                JsonNode block = blockToAnthropic(mapper, b);
                if (block != null) {
                    arr.add(block);
                }
            }
            if (arr.isEmpty() && StringUtils.hasText(m.getContent())) {
                msg.put("content", m.getContent());
            } else if (arr.isEmpty()) {
                msg.put("content", "");
            } else {
                msg.set("content", arr);
            }
            return msg;
        }

        if (m.getContent() != null) {
            msg.put("content", m.getContent());
        } else {
            msg.put("content", "");
        }
        return msg;
    }

    private static ObjectNode toolResultMessage(ObjectMapper mapper, ChatMessage m) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("role", "user");
        ArrayNode content = msg.putArray("content");
        ObjectNode tr = content.addObject();
        tr.put("type", "tool_result");
        tr.put("tool_use_id", m.getToolCallId() != null ? m.getToolCallId() : "");
        boolean isError = false;
        String text = m.getContent() != null ? m.getContent() : "";
        if (!CollectionUtils.isEmpty(m.getBlocks())) {
            ContentBlock b = m.getBlocks().getFirst();
            if ("tool_result".equals(b.getType()) || b.getToolResult() != null) {
                if (b.getToolResult() != null) {
                    text = b.getToolResult();
                }
                if (Boolean.TRUE.equals(b.getIsError())) {
                    isError = true;
                }
            }
        }
        tr.put("content", text);
        if (isError) {
            tr.put("is_error", true);
        }
        return msg;
    }

    private static JsonNode blockToAnthropic(ObjectMapper mapper, ContentBlock b) {
        if (b == null || b.getType() == null) {
            return null;
        }
        return switch (b.getType()) {
            case "text" -> {
                ObjectNode o = mapper.createObjectNode();
                o.put("type", "text");
                o.put("text", b.getText() != null ? b.getText() : "");
                yield o;
            }
            case "thinking" -> {
                ObjectNode o = mapper.createObjectNode();
                o.put("type", "thinking");
                o.put("thinking", b.getText() != null ? b.getText() : "");
                yield o;
            }
            case "tool_use" -> toolUseBlock(mapper, b);
            case "tool_result" -> {
                ObjectNode o = mapper.createObjectNode();
                o.put("type", "tool_result");
                o.put("tool_use_id", b.getToolUseId() != null ? b.getToolUseId() : "");
                o.put("content", b.getToolResult() != null ? b.getToolResult() : "");
                if (Boolean.TRUE.equals(b.getIsError())) {
                    o.put("is_error", true);
                }
                yield o;
            }
            case "image" -> imageBlock(mapper, b);
            default -> {
                if (StringUtils.hasText(b.getText())) {
                    ObjectNode o = mapper.createObjectNode();
                    o.put("type", "text");
                    o.put("text", b.getText());
                    yield o;
                }
                yield null;
            }
        };
    }

    private static ObjectNode toolUseBlock(ObjectMapper mapper, ContentBlock b) {
        ObjectNode o = mapper.createObjectNode();
        o.put("type", "tool_use");
        o.put("id", b.getToolUseId() != null ? b.getToolUseId() : "");
        o.put("name", b.getToolName() != null ? b.getToolName() : "");
        JsonNode input = mapper.createObjectNode();
        if (StringUtils.hasText(b.getToolInput())) {
            try {
                input = mapper.readTree(b.getToolInput());
            } catch (Exception ignored) {
                input = mapper.createObjectNode().put("raw", b.getToolInput());
            }
        }
        o.set("input", input);
        return o;
    }

    private static ObjectNode imageBlock(ObjectMapper mapper, ContentBlock b) {
        ObjectNode o = mapper.createObjectNode();
        o.put("type", "image");
        ObjectNode src = mapper.createObjectNode();
        if (StringUtils.hasText(b.getImageUrl())) {
            src.put("type", "url");
            src.put("url", b.getImageUrl());
        } else {
            src.put("type", "url");
            src.put("url", "");
        }
        o.set("source", src);
        return o;
    }

    private static ObjectNode normalizeTool(ObjectMapper mapper, Map<String, Object> tool) {
        if (tool == null) {
            return mapper.createObjectNode();
        }
        if ("function".equals(String.valueOf(tool.get("type"))) && tool.get("function") instanceof Map<?, ?> fn) {
            @SuppressWarnings("unchecked")
            Map<String, Object> f = (Map<String, Object>) fn;
            return openAiFunctionToAnthropic(mapper, f);
        }
        ObjectNode out = mapper.createObjectNode();
        Object name = tool.get("name");
        out.put("name", name != null ? name.toString() : "tool");
        Object desc = tool.get("description");
        if (desc != null) {
            out.put("description", desc.toString());
        }
        JsonNode schema = parseInputSchema(mapper, tool.get("inputSchema"));
        if (schema == null || schema.isNull()) {
            schema = mapper.createObjectNode().put("type", "object");
        }
        out.set("input_schema", schema);
        return out;
    }

    private static ObjectNode openAiFunctionToAnthropic(ObjectMapper mapper, Map<String, Object> fn) {
        ObjectNode out = mapper.createObjectNode();
        Object name = fn.get("name");
        out.put("name", name != null ? name.toString() : "tool");
        Object desc = fn.get("description");
        if (desc != null) {
            out.put("description", desc.toString());
        }
        JsonNode params = parseInputSchema(mapper, fn.get("parameters"));
        if (params == null || params.isNull()) {
            params = mapper.createObjectNode().put("type", "object");
        }
        out.set("input_schema", params);
        return out;
    }

    private static JsonNode parseInputSchema(ObjectMapper mapper, Object raw) {
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

    private static ObjectNode toolChoiceNode(ObjectMapper mapper, String toolChoice) {
        if (!StringUtils.hasText(toolChoice) || "auto".equalsIgnoreCase(toolChoice)) {
            ObjectNode o = mapper.createObjectNode();
            o.put("type", "auto");
            return o;
        }
        if ("any".equalsIgnoreCase(toolChoice) || "required".equalsIgnoreCase(toolChoice)) {
            ObjectNode o = mapper.createObjectNode();
            o.put("type", "any");
            return o;
        }
        if ("none".equalsIgnoreCase(toolChoice)) {
            ObjectNode none = mapper.createObjectNode();
            none.put("type", "none");
            return none;
        }
        ObjectNode o = mapper.createObjectNode();
        o.put("type", "tool");
        o.put("name", toolChoice);
        return o;
    }
}
