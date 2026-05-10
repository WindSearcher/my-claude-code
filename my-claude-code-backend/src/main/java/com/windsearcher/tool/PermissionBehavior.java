package com.windsearcher.tool;

/** 工具权限检查返回值 — 替代外部依赖中的同名枚举。 */
public enum PermissionBehavior {
    PASSTHROUGH,
    BLOCK,
    DENY,
    ALLOW
}
