package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.windsearcher.tool.BaseTool;
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
public class QueryRequest {
    private String prompt;
    private String model;
    private String systemPrompt;
    private String appendSystemPrompt;
//    PermissionMode permissionMode;
    private List<String> allowedTools;
    private List<String> disallowedTools;

    /**
     * 用户消息
     */
    private String userMessage;

    private String sessionId;

    private Integer maxTurns;

    private String workingDirectory;
}
