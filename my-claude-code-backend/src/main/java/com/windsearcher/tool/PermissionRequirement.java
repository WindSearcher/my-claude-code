package com.windsearcher.tool;

/**
 * 权限需求枚举 — 工具声明是否需要用户确认。
 *
 */
public enum PermissionRequirement {
    /** 无需权限（如文件读取） */
    NONE,
    /** 总是询问 */
    ALWAYS_ASK,
    /** 条件判断（如 Bash 命令需检查内容） */
    CONDITIONAL
}
