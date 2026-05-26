package com.windsearcher.llm;


import com.windsearcher.domain.ChatRequest;
import com.windsearcher.domain.ChatResponse;

import java.util.List;

/**
 * LLM Provider 接口 — 统一多模型供应商的 API 调用。
 * <p>
 * 支持的供应商: OpenAI、Anthropic，即支持该协议的模型。
 * 每个供应商实现各自的请求/响应格式转换，对上层透明。
 * <p>
 * 【架构裁决 #1】使用回调模式替代 Reactor Flux。
 * streamChat() 在当前线程阻塞直到流结束，配合 Virtual Threads 使用。
 * <p>
 */
public interface LlmProvider {

    /**
     * 供应商标识，例如支持openai和anthropic协议的模型
     * **/
    String getProviderName();

    /**
     * 支持的模型列表
     * **/
    List<String> getSupportedModels();

    // ==================== 核心调用 ====================

    /**
     * 流式调用
     */
    ChatResponse streamChat(ChatRequest request);

    /**
     * 同步调用
     */
    ChatResponse chatSync(ChatRequest request);

}
