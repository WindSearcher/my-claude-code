package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工具使用上下文 — 包含工具执行所需的所有环境信息。
 * 每个 query loop 轮次构建一个共享的上下文实例。
 * <p>
 * 映射自 TypeScript {@code ToolUseContext}（claude-code-analysis/src/Tool.ts），
 * 所有回调/方法签名在 Java 层由服务注入，本 DTO 仅保留状态与配置数据。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolUseContext {

    // ==================== Options ====================

    /**
     * 运行配置选项（命令、模型、工具集、预算等）。
     */
    private Options options;

    /**
     * 运行配置选项 — 对应 TypeScript ToolUseContext.options。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Options {

        /** 可用命令列表（原 Command[]，暂以 JsonNode 保持扩展性）。 */
        private List<JsonNode> commands;

        /** 是否开启调试日志。 */
        private Boolean debug;

        /** 主循环使用的模型标识。 */
        private String mainLoopModel;

        /** 可用工具集（原 Tool[]，暂以 JsonNode 保持扩展性）。 */
        private List<JsonNode> tools;

        /** 是否输出详细日志。 */
        private Boolean verbose;

        /** Thinking 配置（原 ThinkingConfig）。 */
        private JsonNode thinkingConfig;

        /** MCP 服务器连接列表。 */
        private List<JsonNode> mcpClients;

        /** MCP 资源映射：serverName → 资源列表。 */
        private Map<String, List<JsonNode>> mcpResources;

        /** 是否为非交互式会话（如 SDK / 后台任务）。 */
        @JsonProperty("isNonInteractiveSession")
        private Boolean nonInteractiveSession;

        /** 代理定义（原 AgentDefinitionsResult）。 */
        private JsonNode agentDefinitions;

        /** 最大预算（USD）。 */
        private Double maxBudgetUsd;

        /** 自定义系统提示（完全替换默认提示）。 */
        private String customSystemPrompt;

        /** 附加系统提示（追加在主提示之后）。 */
        private String appendSystemPrompt;

        /** 查询来源追踪（如 repl、sdk、api）。 */
        private String querySource;
    }

    // ==================== 状态与缓存 ====================

    /**
     * 取消标志 — 对应 TypeScript abortController.signal 的布尔快照。
     * 实际取消语义由上层服务控制。
     */
    @Builder.Default
    private Boolean cancelled = Boolean.FALSE;

    /**
     * 文件读取状态缓存（原 FileStateCache）。
     */
    private JsonNode readFileState;

    // ==================== 消息 ====================

    /**
     * 当前会话消息列表。
     * <p>
     * 对应 TypeScript Message[]，使用项目已有的 ChatMessage 承载。
     */
    private List<ChatMessage> messages;

    // ==================== 会话 / 代理标识 ====================

    /** 会话 ID — 主线程标识符。 */
    private String sessionId;

    /** 代理 ID（仅子代理有值；主线程用 sessionId）。 */
    private String agentId;

    /** 代理类型名称。 */
    private String agentType;

    /** 当前工具调用 ID。 */
    private String toolUseId;

    /** 会话 UUID。 */
    private String conversationId;

    /** 工作目录 — 用于路径解析的安全根目录。 */
    private String workingDirectory;

    /** 查询链追踪信息。 */
    private QueryChainTracking queryTracking;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QueryChainTracking {
        private String chainId;
        private Integer depth;
    }

    // ==================== 集合与标志 ====================

    /** 已附加的嵌套记忆触发器集合。 */
    private Set<String> nestedMemoryAttachmentTriggers;

    /** 本会话已加载的嵌套记忆路径（去重）。 */
    private Set<String> loadedNestedMemoryPaths;

    /** 动态技能目录触发器集合。 */
    private Set<String> dynamicSkillDirTriggers;

    /** 本会话通过 skill_discovery 发现的技能名（仅遥测）。 */
    private Set<String> discoveredSkillNames;

    /** 用户是否已修改过上下文。 */
    private Boolean userModified;

    /**
     * 即使 hooks 自动批准，也必须调用 canUseTool。
     * 用于 speculation 的覆盖文件路径重写。
     */
    private Boolean requireCanUseTool;

    /** 即使子代理也保留 toolUseResult（用于 in-process teammate）。 */
    private Boolean preserveToolUseResults;

    /** 实验性关键系统提醒。 */
    @JsonProperty("criticalSystemReminder_EXPERIMENTAL")
    private String criticalSystemReminderExperimental;

    // ==================== 限制配置 ====================

    /** 文件读取限制。 */
    private FileReadingLimits fileReadingLimits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileReadingLimits {
        private Integer maxTokens;
        private Integer maxSizeBytes;
    }

    /** Glob 搜索限制。 */
    private GlobLimits globLimits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GlobLimits {
        private Integer maxResults;
    }

    // ==================== 决策与跟踪 ====================

    /**
     * 工具决策记录：工具名 → 决策信息。
     */
    private Map<String, ToolDecision> toolDecisions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolDecision {
        private String source;
        private String decision; // "accept" | "reject"
        private Long timestamp;
    }

    /**
     * 本地拒绝跟踪状态（用于异步子代理）。
     */
    private JsonNode localDenialTracking;

    /**
     * 内容替换状态（工具结果预算管理）。
     */
    private JsonNode contentReplacementState;

    /**
     * 父线程在轮次开始时渲染的系统提示字节（冻结状态）。
     * 用于 fork 子代理共享父代理的 prompt cache。
     */
    private JsonNode renderedSystemPrompt;
}
