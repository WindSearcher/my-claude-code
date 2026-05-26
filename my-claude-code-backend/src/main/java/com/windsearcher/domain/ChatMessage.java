package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
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

    private Role role;
    private String content;
    private List<ContentBlock> blocks;
    private List<ToolCall> toolCalls;
    private String toolCallId;
    private String toolName;

}
