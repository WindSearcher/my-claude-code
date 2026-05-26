package com.windsearcher.controller;


import com.windsearcher.domain.*;
import com.windsearcher.engine.QueryEngine;
import com.windsearcher.prompt.SystemPromptBuilder;
import com.windsearcher.prompt.SystemPromptConfig;
import com.windsearcher.tool.BaseTool;
import com.windsearcher.tool.ToolRegistry;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * QueryController — 查询 API (CLI/SDK 专用)。
 * <p>
 * 端点:
 * <ul>
 *   <li>POST /api/query — 同步单次查询</li>
 *   <li>POST /api/query/stream — SSE 流式查询</li>
 *   <li>POST /api/query/conversation — 多轮会话查询</li>
 * </ul>
 * <p>
 */
@RestController
@RequestMapping("/api/query")
public class QueryController {


    @Resource
    private ToolRegistry toolRegistry;

    @Resource
    private SystemPromptBuilder systemPromptBuilder;

    @Resource
    private QueryEngine queryEngine;

    /**
     * POST /api/query — 同步单次查询。
     * <p>
     * 阻塞直到 LLM 完成所有轮次，返回完整结果。
     * 在 Virtual Thread 中执行，不阻塞平台线程。
     * 权限策略: 默认 DONT_ASK (非交互场景)。
     */
    @PostMapping
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {

        // 1.加载会话数据，把同一个会话的数据获取到
        SessionContext sessionContext = null;

        // 2.准备本次会话所需要使用的工具
        List<BaseTool> tools = assembleToolPool(request);

        // 3.准备系统提示词
        SystemPromptConfig promptConfig = SystemPromptConfig.builder().build();
        String systemPrompt = systemPromptBuilder.buildEffectiveSystemPrompt(
                promptConfig, tools, request.getModel(), Path.of(System.getProperty("user.dir"))
        );

        // 4.组装用户消息
        String userMessage = request.getUserMessage();

        // 5.获取用户历史消息
        List<ChatMessage> historyMessages = new ArrayList<>();
        if (StringUtils.isNotEmpty(request.getSessionId())) {
            // 加载用户历史消息
        }

        // 6. 构建 QueryConfig（★ 别名解析: light→qwen-plus, standard→qwen3.6-plus, premium→qwen3.7-max）
//        String rawModel = request.model();
//        String model = providerRegistry.resolveModelAlias(rawModel);
        String model = request.getModel();
        int maxTurns = request.getMaxTurns() != null ? request.getMaxTurns() : QueryConfig.DEFAULT_MAX_TURNS;
        // 根据模型获取上下文长度，先默认时200KB
        int contextWindow = 1024 * 200;


        QueryConfig config = QueryConfig.withDefaults(
                model, systemPrompt, tools,
                tools.stream().map(BaseTool::toToolDefinition).toList(),
                QueryConfig.DEFAULT_MAX_TOKENS,
                contextWindow,
                maxTurns, "rest-api"
        );

        String effectiveWorkDir = request.getWorkingDirectory();

        ToolUseContext toolCtx = ToolUseContext.builder()
                .messages(historyMessages)
                .sessionId(request.getSessionId())
                .build();

        QueryLoopState state = new QueryLoopState(historyMessages, toolCtx);

        // 7.执行查询引擎
        QueryResult queryResult = queryEngine.execute(config, state);

        return null;
    }

    private List<BaseTool> assembleToolPool(QueryRequest request) {
        List<BaseTool> tools = toolRegistry.getEnabledTools();
        List<String> disallowedTools = request.getDisallowedTools();
        List<String> allowedTools = request.getAllowedTools();

        if (allowedTools != null && !allowedTools.isEmpty()) {
            Set<String> allowed = Set.copyOf(allowedTools);
            tools = tools.stream().filter(t -> allowed.contains(t.getName())).toList();
        }
        if (disallowedTools != null && !disallowedTools.isEmpty()) {
            Set<String> denied = Set.copyOf(disallowedTools);
            tools = tools.stream().filter(t -> !denied.contains(t.getName())).toList();
        }
        return tools;
    }
}
