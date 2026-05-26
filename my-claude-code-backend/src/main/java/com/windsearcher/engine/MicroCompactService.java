package com.windsearcher.engine;

import com.windsearcher.domain.ChatMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 微压缩服务 — 基于 COMPACTABLE_TOOLS 白名单清除旧工具结果内容。
 * <p>
 * {@code [Old tool result content cleared]}，释放 token 空间。
 * 保护最近 N 条消息不被清除 (protected tail)。
 *
 */
@Service
@Slf4j
public class MicroCompactService {

    static final String CLEARED_MESSAGE = "[Old tool result content cleared]";

    /** 可微压缩的工具集合  */
    static final Set<String> COMPACTABLE_TOOLS = Set.of(
            "Read", "Bash", "Grep", "Glob",
            "WebSearch", "WebFetch", "Edit", "Write"
    );

    @Resource
    private TokenCounter tokenCounter;

    public List<ChatMessage> compactMessages(List<ChatMessage> messages, int protectedTailSize) {

        int tokensFreed = 0;
        int boundary = Math.max(0, messages.size() - protectedTailSize);
        List<ChatMessage> result = new ArrayList<>(messages.size());
        int clearedCount = 0;

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);

            // 处于保护窗口期内
            if (i >= boundary) {
                result.add(msg);
                continue;
            }

            if (msg instanceof Message.UserMessage user
                    && user.toolUseResult() != null
                    && !user.toolUseResult().equals(CLEARED_MESSAGE)) {
                // 检查是否属于可压缩工具 — 通过预扫描的 ID 集合判断
                boolean isCompactable = isCompactableByIds(user, compactableIds, messages);
                if (isCompactable) {
                    tokensFreed += tokenCounter.estimateTokens(user.toolUseResult());
                    result.add(new Message.UserMessage(
                            user.uuid(), user.timestamp(), user.content(),
                            CLEARED_MESSAGE, user.sourceToolAssistantUUID()));
                    clearedCount++;
                } else {
                    result.add(msg);
                }
            } else {
                result.add(msg);
            }
        }

        if (tokensFreed > 0) {
            log.info("MicroCompact: cleared {} old tool results, freed ~{} tokens",
                    clearedCount, tokensFreed);
        }

        return result;
    }
}
