package com.windsearcher.llm;

/**
 * 流式 LLM 响应下沉 — OpenAI Chat Completions SSE 与 Anthropic Messages SSE
 * 统一为文本增量、推理增量、工具调用与用量回调。
 * <p>
 * 实现类应保证线程安全若回调来自 OkHttp 读取线程以外；当前实现均在阻塞读线程调用。
 */
public interface LlmStreamSink {

    void onTextDelta(String fragment);

    default void onReasoningDelta(String fragment) {
    }

    /**
     * OpenAI / 兼容接口：工具调用开始（同一 id 可能多次 delta arguments）。
     */
    default void onToolCallStart(String id, String name) {
    }

    default void onToolCallArgumentsDelta(String id, String jsonFragment) {
    }

    default void onUsage(int inputTokens, int outputTokens) {
    }

    /**
     * @param stopReason OpenAI: finish_reason；Anthropic: stop_reason 映射后的简短标记
     */
    void onMessageComplete(String stopReason);

    default void onError(String message, boolean retryable) {
    }
}
