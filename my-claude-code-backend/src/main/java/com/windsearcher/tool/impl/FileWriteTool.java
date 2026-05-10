package com.windsearcher.tool.impl;

/**
 * FileWriteTool — 创建或覆盖文件（全量写入）。
 * <p>
 * 安全机制: Read-before-Edit (文件必须先通过 FileReadTool 读取)
 * + mtime 竞态检测 (防止外部修改覆盖)。
 * </p>
 */
public class FileWriteTool {
}
