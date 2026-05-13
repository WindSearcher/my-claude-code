package com.windsearcher.domain;


import java.util.List;
import java.util.Map;

/**
 * 全局用户配置。
 *
 */
public record UserConfig(
        // 认证
        String authType,
        String apiKey,
//        OAuthToken oauthToken,

        // 模型
        String defaultModel,
        Map<String, String> modelAliases,

        // 外观
        String theme,
        String locale,

        // 权限
//        PermissionMode defaultPermissionMode,
//        List<PermissionRule> globalAlwaysAllowRules,
//        List<PermissionRule> globalAlwaysDenyRules,

        // MCP
//        Map<String, McpServerConfig> mcpServers,

        // 其他
        boolean analyticsEnabled,
        boolean autoCompactEnabled,
        int autoCompactThreshold
) {}
