package com.windsearcher.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
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

}
