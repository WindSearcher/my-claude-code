package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "LLM 查询请求")
public class QueryRequest {
    @Schema(description = "用户输入提示词", example = "帮我分析当前项目结构")
    private String prompt;

    @Schema(description = "模型名称或模型别名", example = "qwen3.7-max")
    private String model;

    @Schema(description = "覆盖默认系统提示词")
    private String systemPrompt;

    @Schema(description = "追加到默认系统提示词后的内容")
    private String appendSystemPrompt;
//    PermissionMode permissionMode;

    @Schema(description = "只允许使用的工具名称列表")
    private List<String> allowedTools;

    @Schema(description = "禁止使用的工具名称列表")
    private List<String> disallowedTools;

    /**
     * 用户消息
     */
    @Schema(description = "用户消息", example = "请读取 README 并总结项目启动方式")
    private String userMessage;

    @Schema(description = "会话 ID，用于多轮上下文", example = "session-001")
    private String sessionId;

    @Schema(description = "最大推理轮次", example = "10")
    private Integer maxTurns;

    @Schema(description = "工具执行工作目录", example = "/Users/jichen.lq/code/my-claude-code")
    private String workingDirectory;
}
