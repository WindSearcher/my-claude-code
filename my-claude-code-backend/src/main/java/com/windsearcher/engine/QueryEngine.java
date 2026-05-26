package com.windsearcher.engine;

import com.alibaba.fastjson2.JSONObject;
import com.windsearcher.domain.*;
import com.windsearcher.llm.LlmProvider;
import com.windsearcher.llm.LlmProviderRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 查询引擎 - claude code核心，Agent Loop的核心实现入口
 */
@Service
@Slf4j
public class QueryEngine {

    @Resource
    private SessionContextCompress sessionContextCompress;

    @Resource
    private LlmProviderRegistry providerRegistry;

    /**
     * 执行agent loop循环流水线：
     * 1.
     * @return
     */
    public QueryResult execute(QueryConfig config, QueryLoopState state) {

        log.info("QueryEngine 开始执行: model={}, maxTokens={}, maxTurns={}",
                config.getModel(), config.getMaxTokens(), config.getMaxTurns());

        String sessionId = state.getToolUseContext() != null
                ? state.getToolUseContext().getSessionId() : null;

        QueryResult queryResult = new QueryResult();
        TokenUsage tokenUsage = new TokenUsage();

        try {
            tokenUsage = queryLoop(config, state);
        } catch (Throwable t) {
            log.error("QueryEngine error, config={}, state={}",
                    JSONObject.toJSONString(config), JSONObject.toJSONString(state), t);
        } finally {

        }

        return queryResult;
    }


    /**
     * 核心查询循环 — 8 步迭代。
     * 常用的上下文压缩：
     *
     */
    private TokenUsage queryLoop(QueryConfig config, QueryLoopState state) {

        // agent loop，agent的核心
        while (true) {
            state.incrementTurnCount();

            // step 1：上下文压缩
            sessionContextCompress.compress(state.getMessages(), config.getModel());

            // step 2：

            // step 3：模型调用
            LlmProvider provider = providerRegistry.getProvider(config.getModel());


        }
    }
}
