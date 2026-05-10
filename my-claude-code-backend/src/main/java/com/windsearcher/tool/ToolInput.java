package com.windsearcher.tool;

import java.util.Collections;
import java.util.Map;

/** 工具调用入参占位 — 后续可由管线填充。 */
public record ToolInput(Map<String, Object> arguments) {
    public ToolInput {
        arguments = arguments == null ? Collections.emptyMap() : Map.copyOf(arguments);
    }

    public static ToolInput empty() {
        return new ToolInput(Map.of());
    }
}
