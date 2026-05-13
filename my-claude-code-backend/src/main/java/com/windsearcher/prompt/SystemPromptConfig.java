package com.windsearcher.prompt;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统提示词的配置，用于SystemPromptBuilder的5级优先链选择
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemPromptConfig {

    /**
     * 优先级0：override系统提示（最高优先级）
     */
    private String overrideSystemPrompt;

    /**
     * 优先级1：coordinator，多Agent协作模式专用
     */
    private String coordinatorPrompt;

    /**
     * 优先级3：custom系统提示（--system-prompt CLI参数），用户在配置里自定义的
     */
    private String customSystemPrompt;

    /**
     * 追加到最终提示末尾（除非使用override）
     */
    private String appendSystemPrompt;

    /**
     * 会话ID（用于coordinator模式动态生成提示词）
     */
    private String sessionId;

}
