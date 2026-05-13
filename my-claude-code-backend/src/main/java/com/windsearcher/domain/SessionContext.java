package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionContext {

    /**
     * 会话ID
     */
    private String sessionId;

    private String model;

    private String workingDir;

    private String title;

    private String status;

    private List<ChatMessage> messages;

    private Map<String, Object> config;

    private TokenUsage tokenUsage;

    private Double totalCostUsd;

    private String summary;

    /**
     * 创建时间
     */


    /**
     * 修改时间
     */

}
