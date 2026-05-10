package com.windsearcher.controller;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QueryController — 查询 API (CLI/SDK 专用)。
 * <p>
 * 端点:
 * <ul>
 *   <li>POST /api/query — 同步单次查询</li>
 *   <li>POST /api/query/stream — SSE 流式查询</li>
 *   <li>POST /api/query/conversation — 多轮会话查询</li>
 * </ul>
 * <p>
 */
@RestController
@RequestMapping("/api/query")
public class QueryController {


}
