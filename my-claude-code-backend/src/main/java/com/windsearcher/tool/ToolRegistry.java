package com.windsearcher.tool;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ToolRegistry {

    private final Map<String, BaseTool> toolsByName = new ConcurrentHashMap<>();
    private final Map<String, BaseTool> toolsByAlias = new ConcurrentHashMap<>();

    /** Caffeine 缓存：排序后的工具列表（key = sessionId::toolSetHash） */
    private final Cache<String, List<BaseTool>> sortedToolCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 构造函数 — 通过 Spring 自动注入所有 Tool 实现。
     */
    public ToolRegistry(List<BaseTool> tools) {
        for (BaseTool tool : tools) {
            register(tool);
        }
        log.info("ToolRegistry initialized with {} tools: {}",
                toolsByName.size(), toolsByName.keySet());
    }

    /** 注册工具 */
    public void register(BaseTool tool) {
        toolsByName.put(tool.getName(), tool);
        for (String alias : tool.getAliases()) {
            toolsByAlias.put(alias, tool);
        }
    }

    /** 注销工具 */
    public void unregister(String toolName) {
        BaseTool removed = toolsByName.remove(toolName);
        if (removed != null) {
            for (String alias : removed.getAliases()) {
                toolsByAlias.remove(alias);
            }
        }
    }

    /** 按名称查找工具（含别名） */
    public BaseTool findByName(String name) {
        BaseTool tool = toolsByName.get(name);
        if (tool != null) return tool;
        tool = toolsByAlias.get(name);
        if (tool != null) return tool;
        throw new IllegalArgumentException("Unknown tool: " + name);
    }

    /** 列出所有已注册的工具 */
    public List<BaseTool> getAllTools() {
        return List.copyOf(toolsByName.values());
    }

    /** 列出启用的工具 */
    public List<BaseTool> getEnabledTools() {
        return toolsByName.values().stream()
                .filter(BaseTool::isEnabled)
                .toList();
    }
}
