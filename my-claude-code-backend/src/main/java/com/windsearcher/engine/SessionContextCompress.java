package com.windsearcher.engine;


import com.windsearcher.domain.ChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SessionContextCompress — 上下文压缩，从浅入深压缩
 * 从两个维度来看会话级上下文的压缩：
 * - 当前正在执行的工具：在工具里面做？
 * - 历史的消息和工具
 * <p>
 * <pre>
 *   Level 0: Snip           ← 单条工具结果超预算时，截断中间保留首尾
 *   Level 1: MicroCompact   ← 对旧的可压缩工具结果替换为 "[cleared]"
 *   Level 2: AutoCompact    ← token 率 > 阈值时，三区划分 + LLM 摘要
 *   Level 3: CollapseDrain  ← 紧急情况下激进压缩 (contextWindow*0.5 目标)
 *   Level 4: ReactiveCompact← API 返回 413 时，仅保留 1 轮 + 极度压缩
 * </pre>
 * <p>
 * 关键设计：
 * - Level 0-1 每次 API 调用前无条件执行（代价极低）
 * - Level 2 基于 buffer-based 阈值触发
 * - Level 3-4 仅在错误恢复路径触发
 * - 状态通过 CascadeState 在层级间传递
 *
 */
@Service
@Slf4j
public class SessionContextCompress {

    @Resource
    private SnipService snipService;

    /** 工具结果预算占上下文窗口比例 */
    private static final double TOOL_RESULT_BUDGET_RATIO = 0.3;

    public void compress(List<ChatMessage> messages, String model) {

//        int contextWindow = modelRegistry.getContextWindowForModel(model);
        // 模型的上下文窗口大小
        int contextWindow = 1024 * 200;



        // ===== Level 0: Snip (单条工具超预算，其结果进行截断) =====
        int toolResultBudget = (int) (contextWindow * TOOL_RESULT_BUDGET_RATIO);
        List<ChatMessage> afterSnip = snipService.snipToolResults(messages, toolResultBudget);
//        int snipBefore = tokenCounter.estimateTokens(messages);
//        int snipAfter = tokenCounter.estimateTokens(afterSnip);
//        if (snipAfter < snipBefore) {
//            snipExecuted = true;
//            snipTokensFreed = snipBefore - snipAfter;
//            current = afterSnip;
//            log.debug("Level 0 Snip: freed {} tokens", snipTokensFreed);
//        }

        // ===== Level 1: MicroCompact (旧工具结果清除) =====


        // ===== Level 3: AutoCompact (LLM 摘要) — 含 Collapse 互斥协调 =====
    }
}
