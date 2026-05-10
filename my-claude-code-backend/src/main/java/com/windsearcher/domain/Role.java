package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * ChatMessage消息角色枚举
 */
public enum Role {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    /** 工具结果（OpenAI 为 tool；Anthropic 常以 user + tool_result 块表达） */
    TOOL("tool");

    private final String apiName;

    Role(String apiName) {
        this.apiName = apiName;
    }

    @JsonValue
    public String apiName() {
        return apiName;
    }
}
