package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    private Role role;

    /**
     * 纯文本
     */
    private String content;

    private List<ContentBlock> blocks;

    /**
     * 当role=tool时关联的工具调用ID
     */
    private String toolCallId;

    /**
     * 当role=tool时关联的工具名
     */
    private String toolName;

}
