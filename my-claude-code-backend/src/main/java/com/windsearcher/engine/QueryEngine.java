package com.windsearcher.engine;

import com.alibaba.fastjson2.JSONObject;
import com.windsearcher.domain.QueryConfig;
import com.windsearcher.domain.QueryLoopState;
import com.windsearcher.domain.QueryResult;
import com.windsearcher.domain.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 查询引擎 - claude code核心，Agent Loop的核心实现入口
 */
@Service
@Slf4j
public class QueryEngine {

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

            // step 2：

            // step 3：模型调用


        }
    }
}
