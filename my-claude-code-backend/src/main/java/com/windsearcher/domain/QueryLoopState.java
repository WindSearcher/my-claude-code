package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.windsearcher.exception.LlmApiException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryLoopState {

    private List<ChatMessage> messages;
    private ToolUseContext toolUseContext;
    private boolean autoCompactEnabled = true;
    private int autoCompactFailures = 0;
    private int maxOutputTokensRecoveryCount = 0;
    private Integer maxTokensOverride = null;
    private boolean hasAttemptedReactiveCompact = false;
    private int turnCount = 0;
//    private AbortReason abortReason = null;
    private boolean stopHookActive = false;
    private String lastTransitionReason = null;

    // === SelfCorrectionLoop 状态字段 ===
    private int correctionAttempts = 0;
    private String previousToolOutput = null;

    /** 扣留的错误 — 413/max_output_tokens 等可恢复错误在恢复尝试期间扣留，不立即释放给消费者 */
    private List<LlmApiException> withheldErrors = new ArrayList<>();

    /** 413错误扣留状态 — true表示正在恢复中，不向消费者暴露错误 */
    @com.fasterxml.jackson.annotation.JsonProperty("promptTooLongWithheld")
    private boolean promptTooLongWithheld = false;

    /** 增量折叠标记，标识当前轮次是否需要增量折叠 */
    @com.fasterxml.jackson.annotation.JsonProperty("incrementalCollapseNeeded")
    private boolean incrementalCollapseNeeded = false;

    public QueryLoopState(List<ChatMessage> messages, ToolUseContext toolUseContext) {
        this.messages = new ArrayList<>(messages);
        this.toolUseContext = toolUseContext;
    }

    public void incrementTurnCount() {
        this.turnCount++;
    }

}
