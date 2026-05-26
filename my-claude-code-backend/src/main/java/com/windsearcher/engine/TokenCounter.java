package com.windsearcher.engine;

import com.windsearcher.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Token 计数器 — 多层精度估算。
 * <p>
 * 三层精度策略：
 * 1. 粗略估算: 基于字符数的快速估算（默认）
 * 2. 文件类型调整: 根据内容类型使用不同系数
 * 3. 精确计算: 通过 Python tiktoken 服务（可选，关键路径使用）
 */
@Component
public class TokenCounter {

    private static final Logger log = LoggerFactory.getLogger(TokenCounter.class);

    /** 默认每 token 字符数（英文约4，中文约2，混合取3.5） */
    private static final double DEFAULT_CHARS_PER_TOKEN = 3.5;

    /** JSON 内容每 token 字符数（JSON 结构化更紧凑） */
    private static final double JSON_CHARS_PER_TOKEN = 2.0;

    /** 代码内容每 token 字符数 */
    private static final double CODE_CHARS_PER_TOKEN = 3.5;

    /** 自然语言每 token 字符数 */
    private static final double NATURAL_LANGUAGE_CHARS_PER_TOKEN = 4.0;

    /** 中文内容每 token 字符数 */
    private static final double CHINESE_CHARS_PER_TOKEN = 2.0;

    private static final Set<String> CODE_KEYWORDS = Set.of(
            "import ", "function ", "class ", "def ",
            "public ", "private ", "const ", "let ", "var ", "return "
    );

    // ===== 公开 API =====

    /**
     * 估算消息列表的总 token 数。
     */
    public int estimateTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        var totalChars = messages.stream()
                .mapToInt(this::estimateMessageChars)
                .sum();
        // 消息边界开销: 每条消息约 4 token
        return (int) (totalChars / DEFAULT_CHARS_PER_TOKEN) + messages.size() * 4;
    }

    /**
     * 估算图片 token 数 — 基于实际尺寸计算。
     * <p>
     * 公式: ceil(width * height / 750)
     * 参考 Anthropic Claude 图片 token 计算规则。
     *
     * @param width  图片宽度(像素)
     * @param height 图片高度(像素)
     * @return 估算的 token 数
     */
    public int estimateImageTokens(int width, int height) {
        return (width <= 0 || height <= 0)
                ? 85 // 回退到默认值
                : (int) Math.ceil((double) (width * height) / 750.0);
    }

    /**
     * 检测文本内容类型。
     *
     * @param text 待检测文本
     * @return 内容类型标识: "json", "code", "chinese", "text"
     */
    public String detectContentType(String text) {
        if (text == null || text.length() < 10) {
            return "text";
        }
        var trimmed = text.trim();
        if (isJson(trimmed)) {
            return "json";
        }
        if (chineseRatio(text) > 0.3) {
            return "chinese";
        }
        if (looksLikeCode(trimmed)) {
            return "code";
        }
        return "text";
    }

    // ===== 内部方法 =====

    /**
     * 自动检测内容类型并返回合适的字符/token比率。
     */
    private double detectCharsPerToken(String text) {
        if (text.length() < 10) {
            return DEFAULT_CHARS_PER_TOKEN;
        }
        var trimmed = text.trim();
        if (isJson(trimmed)) {
            return JSON_CHARS_PER_TOKEN;
        }
        var ratio = chineseRatio(text);
        if (ratio > 0.3) {
            // 混合内容：按中文比例加权
            return CHINESE_CHARS_PER_TOKEN * ratio + DEFAULT_CHARS_PER_TOKEN * (1 - ratio);
        }
        if (looksLikeCode(trimmed)) {
            return CODE_CHARS_PER_TOKEN;
        }
        return DEFAULT_CHARS_PER_TOKEN;
    }

    private static boolean isJson(String trimmed) {
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private static double chineseRatio(String text) {
        long chineseChars = text.codePoints()
                .filter(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN)
                .count();
        return (double) chineseChars / text.length();
    }

    /**
     * 启发式检测文本是否看起来像代码。
     * 检查常见代码特征: 花括号、分号、import/function/class 关键字等。
     */
    private boolean looksLikeCode(String text) {
        // 取前 500 字符进行检测，避免大文本性能问题
        var sample = text.length() > 500 ? text.substring(0, 500) : text;

        var codeIndicators = 0;

        // 花括号和分号密度
        var braces = sample.chars().filter(c -> c == '{' || c == '}').count();
        var semicolons = sample.chars().filter(c -> c == ';').count();
        if (braces > 2 || semicolons > 3) {
            codeIndicators++;
        }

        // 常见代码关键字
        if (CODE_KEYWORDS.stream().anyMatch(sample::contains)) {
            codeIndicators++;
        }

        // 缩进模式（连续空格开头行）
        var indentedLines = sample.lines()
                .filter(line -> line.startsWith("    ") || line.startsWith("\t"))
                .count();
        if (indentedLines > 3) {
            codeIndicators++;
        }

        return codeIndicators >= 2;
    }

    /**
     * 估算单条消息的字符数。
     */
    private int estimateMessageChars(ChatMessage message) {
//        if (Role.USER.getType().equals(message.getRole().getType())) {
//            return textAndBlocksLength(message.getContent(), message.getBlocks());
//        } else if (Role.ASSISTANT.getType().equals(message.getRole().getType())) {
//            return textAndBlocksLength(message.getContent(), message.getBlocks());
//        } else if (Role.SYSTEM.getType().equals(message.getRole().getType())) {
//            return lengthOf(message.getContent());
//        } else if (Role.TOOL.getType().equals(message.getRole().getType())) {
//            return textAndBlocksLength(message.getContent(), message.getBlocks());
//        }

        return 0;
    }

    private int textAndBlocksLength(String text, List<ContentBlock> blocks) {
        var chars = lengthOf(text);
        if (blocks != null) {
            for (var block : blocks) {
                chars += estimateBlockChars(block);
            }
        }
        return chars;
    }

    private static int lengthOf(String text) {
        return text != null ? text.length() : 0;
    }

    /**
     * 估算内容块的字符数（增强版）。
     */
    private int estimateBlockChars(ContentBlock block) {
        return Optional.ofNullable(block)
                .map(ContentBlock::getType)
                .map(type -> switch (type) {
                    case "text", "thinking" -> lengthOf(block.getText());
                    case "tool_use" ->
                            lengthOf(block.getToolName())
                                    + lengthOf(block.getToolInput())
                                    + 20; // JSON 结构开销
                    case "tool_result" ->
                            lengthOf(block.getToolResult()) + 10;
                    case "image" -> {
                        // 当前 ContentBlock 没有尺寸信息，使用默认估算
                        var tokens = estimateImageTokens(0, 0);
                        yield (int) (tokens * DEFAULT_CHARS_PER_TOKEN);
                    }
                    default -> lengthOf(block.getText());
                })
                .orElse(0);
    }
}
