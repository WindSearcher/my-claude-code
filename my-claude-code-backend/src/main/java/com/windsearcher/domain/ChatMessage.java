package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    /**
     * 消息ID，包含system、user和assistant
     * **/
    private String messageId;

    /**
     * 消息产生的时间戳
     */
    private Instant timestamp;

    /**
     * 消息角色
     */
    private Role role;

    /**
     * 如果是assistant类型的消息，需要注意其调用了哪些工具
     */
    private String content;

    /**
     * 如果是assistant类型的消息且为思考类模型情况下，会存在值
     */
    private String reasoningContent;

    /**
     * toolCalls，assistant消息才有工具调用结果
     * 每一个工具调用结果，在openai协议的请求体中是作为
     * 一个role为tool的角色
     */
    private List<ToolCall> toolCalls;

    /**
     * 当role为tool时，会有下面三个字段
     */
    private String toolCallId;
    private String toolName;
    private String toolResult;
}
