package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssistantMessage implements ChatMessage {
    private Role role = Role.ASSISTANT;
    private String content;
    private List<ContentBlock> blocks;
    private List<ToolCall> toolCalls;

    public AssistantMessage() {
        this.role = Role.ASSISTANT;
    }
}

