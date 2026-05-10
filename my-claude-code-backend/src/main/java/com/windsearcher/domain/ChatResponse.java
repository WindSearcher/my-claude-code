package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次 Chat Completions / 流式聚合后的统一结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    /**
     * 模型调用ID
     */
    private String id;

    /**
     * 使用的模型
     */
    private String model;

    /** 通常为 assistant */
    private String role;

    /**
     * 模型返回的文本内容
     */
    private String content;

    /**
     * 模型返回的推理内容
     */
    private String reasoningContent;

    @Builder.Default
    private List<ToolCall> toolCalls = new ArrayList<>();

    /** OpenAI: finish_reason；与流式 sink 语义对齐的简短标记 */
    private String finishReason;

    /** 可选：完整响应 JSON（同步或流结束后的最后快照） */
    private JsonNode rawJson;

    private TokenUsage tokenUsage;
}
