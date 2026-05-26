package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemMessage implements ChatMessage {
    private Role role = Role.SYSTEM;
    private String content;

    public SystemMessage() {
        this.role = Role.SYSTEM;
    }
}