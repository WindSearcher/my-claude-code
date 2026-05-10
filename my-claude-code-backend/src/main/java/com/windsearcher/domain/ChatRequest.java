package com.windsearcher.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.windsearcher.llm.LlmStreamSink;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    /**
     * 模型名
     */
    private String model;

    /**
     * 模型协议提供商名称
     */
    private String providerName;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 对话消息数组
     */
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();


    /**
     * 工具列表，传入字符串类型；字符串的json结构如下：
     * [
     *  {
     *      "name":"",
     *      "description":"",
     *      "inputSchema":""
     *  }
     * ]
     */
    private List<Map<String, Object>> tools;

    /**
     * 工具选择策略：auto / any / none / 工具名
     */
    private String toolChoice;


    /**
     * 最大输出token数
     */
    private Integer maxTokens;

    /**
     * 采样温度
     */
    private Double temperature;


    /**
     * Top_p采样
     */
    private Double topP;


    /**
     * 停止序列
     */
    private List<String> stopSequences;

    /**
     * 是否流式
     */
    private boolean stream = false;

    /**
     * 透传的额外的metadata到provider
     */
    private Map<String, Object> metadata;

    /**
     * 调用模型层的超时时间
     */
    private Long timeoutMs;

    /**
     * 流式输出回调（仅流式调用使用；不参与 JSON 反序列化）。
     */
    @JsonIgnore
    private LlmStreamSink streamSink;

}
