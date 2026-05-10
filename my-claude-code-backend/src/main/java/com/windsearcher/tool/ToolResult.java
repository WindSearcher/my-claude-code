package com.windsearcher.tool;

/** 工具执行结果占位。 */
public record ToolResult(boolean ok, String content, String errorMessage) {
    public static ToolResult ok(String content) {
        return new ToolResult(true, content == null ? "" : content, null);
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, "", message);
    }
}
