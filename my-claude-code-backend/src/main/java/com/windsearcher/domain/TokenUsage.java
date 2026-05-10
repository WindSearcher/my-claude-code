package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token用量统计 - 抽象自Anthropic的BetaUsage、OpenAI与Qwen的usage字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenUsage {

    /**
     * 输入的 Token 数；openai协议为prompt_tokens
     */
    private Integer inputTokens;

    /**
     * 模型输出的 Token 数
     */
    private Integer completionTokens;

    /**
     * 消耗的总 Token 数，为 prompt_tokens 与 completion_tokens 的总和
     */
    private Integer totalTokens;

//    /**
//     * 使用 Qwen-VL 模型时输出 Token 的细粒度分类
//     */
    private CompletionTokensDetails completionTokensDetails;

    /**
     * 输入 Token 的细粒度分类
     */
    @JsonProperty("prompt_tokens_details")
    private InputTokensDetails inputTokensDetails;

//    /**
//     * 显式缓存创建信息
//     */
//    @JsonProperty("cache_creation")
//    private CacheCreation cacheCreation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompletionTokensDetails {

        /**
         * 音频 Token 数（当前固定为 null）
         */
        @JsonProperty("audio_tokens")
        private Integer audioTokens;

        /**
         * 推理 Token 数（当前固定为 null）
         */
        @JsonProperty("reasoning_tokens")
        private Integer reasoningTokens;

        /**
         * Qwen-VL 模型输出文本的 Token 数
         */
        @JsonProperty("text_tokens")
        private Integer textTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InputTokensDetails {

        /**
         * 音频 Token 数（当前固定为 null）
         */
        private Integer audioTokens;

        /**
         * 命中 Cache 的 Token 数
         */
        private Integer cachedTokens;


        /**
         * Qwen-VL 模型输入的图像 Token 数
         */
        private Integer imageTokens;

        /**
         * Qwen-VL 模型输入的视频文件或者图像列表 Token 数
         */
        private Integer videoTokens;
    }

//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    public static class CacheCreation {
//
//        /**
//         * 创建显式缓存的 Token 数
//         */
//        @JsonProperty("cache_creation_input_tokens")
//        private Integer cacheCreationInputTokens;
//
//        /**
//         * 使用显式缓存时，参数值为 ephemeral，否则该参数不存在
//         */
//        @JsonProperty("cache_type")
//        private String cacheType;
//    }
}
