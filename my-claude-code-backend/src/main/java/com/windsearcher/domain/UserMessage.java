package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserMessage implements ChatMessage {

    private Role role = Role.USER;
    private String content;
    private List<ContentBlock> blocks;
    private String toolCallId;
    private String toolName;

    public UserMessage() {
        this.role = Role.USER;
    }
}