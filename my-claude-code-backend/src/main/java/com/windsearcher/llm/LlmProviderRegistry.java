package com.windsearcher.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM Provider 注册表 — 管理多供应商实例。
 * <p>
 * 根据模型名称查找对应供应商，支持动态注册。
 * 注册来源：yaml文件或者db
 */
@Service
@Slf4j
public class LlmProviderRegistry {

    /**
     * key：不同厂商名称 + 模型名称
     */
    private final Map<String, LlmProvider> providers = Collections.synchronizedMap(new LinkedHashMap<>());

    /** 内置别名映射（模型层级别名 → 实际部署模型） */
    private static final Map<String, String> BUILTIN_ALIASES = Map.ofEntries(
            // 新别名（推荐）
            Map.entry("light", "qwen-plus"),
            Map.entry("standard", "qwen3.6-plus"),
            Map.entry("premium", "qwen3.7-max")
    );

    /**
     * 构造函数 — 通过 Spring 注入 MultiProviderConfiguration 创建的 Provider 列表。
     */
    public LlmProviderRegistry(
            @Qualifier("openAiCompatibleProviders") List<LlmProvider> providerList,
            Environment env) {
        for (LlmProvider provider : providerList) {
            register(provider);
        }
        log.info("LlmProviderRegistry initialized with {} providers: {}",
                this.providers.size(), this.providers.keySet());
    }

    /** 注册供应商 */
    public void register(LlmProvider provider) {
        providers.put(provider.getProviderName(), provider);
        log.info("Registered LLM provider: {} (models: {})",
                provider.getProviderName(), provider.getSupportedModels());
    }

    /** 根据模型名称查找对应供应商 */
    public LlmProvider getProvider(String model) {
        return providers.values().stream()
                .filter(p -> p.getSupportedModels().contains(model))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No provider found for model: " + model));
    }
}
