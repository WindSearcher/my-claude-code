package com.windsearcher.domain;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.windsearcher.tool.BaseTool;
import lombok.*;

import java.util.List;
import java.util.Map;


/**
 *  查询配置，静态配置，不可修改
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryConfig {

    /** 默认最大输出 token */
    public static final int DEFAULT_MAX_TOKENS = 8192;

    /** 升级后的最大输出 token */
    public static final int ESCALATED_MAX_TOKENS = 65536;

    /** 最大循环轮次 */
    public static final int DEFAULT_MAX_TURNS = 200;

    /** 最大输出恢复次数 */
    public static final int MAX_OUTPUT_TOKENS_RECOVERY_LIMIT = 3;

    private String model;
    private String fallbackModel;
    private String systemPrompt;
    private List<BaseTool> tools;
    private List<Map<String, Object>> toolDefinitions;

    /**
     * 默认的最大输出token
     */
    private int maxTokens;
    private int contextWindow;
//    private ThinkingConfig thinkingConfig;
    private int maxTurns;
    private String querySource;
    private Integer tokenBudget;
    private List<String> modelTierChain;

    /**
     * 向后兼容工厂方法 — 无 fallbackModel 和 tokenBudget。
     * 所有现有 9 参数构造点使用此方法，避免逐个修改。
     */
    public static QueryConfig withDefaults(
            String model, String systemPrompt, List<BaseTool> tools,
            List<Map<String, Object>> toolDefinitions, int maxTokens,
            int contextWindow,
            int maxTurns, String querySource) {
        return new QueryConfig(model, null, systemPrompt, tools,
                toolDefinitions, maxTokens, contextWindow,
                maxTurns, querySource, null, List.of());
    }
}
