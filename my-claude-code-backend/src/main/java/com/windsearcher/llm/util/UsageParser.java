package com.windsearcher.llm.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.windsearcher.domain.TokenUsage;

/**
 * 通用 usage JSON 解析器 — 兼容 OpenAI / Qwen 与 Anthropic 的字段命名差异。
 */
public final class UsageParser {

    private UsageParser() {
    }

    /**
     * 从 JSON 节点解析 {@link TokenUsage}。
     * <p>
     * 支持的字段映射：
     * <ul>
     *   <li>OpenAI / Qwen：prompt_tokens、completion_tokens、total_tokens</li>
     *   <li>Anthropic：input_tokens、output_tokens</li>
     *   <li>Details：prompt_tokens_details、completion_tokens_details</li>
     *   <li>Cache：cache_creation</li>
     * </ul>
     *
     * @param usage 响应中的 usage 节点，允许为 null
     * @return 解析后的 TokenUsage，若节点为空则返回 null
     */
    public static TokenUsage parseUsage(JsonNode usage) {
        if (usage == null || usage.isNull()) {
            return null;
        }

        TokenUsage.TokenUsageBuilder builder = TokenUsage.builder();

        // OpenAI / Qwen 字段名
//        if (usage.has("prompt_tokens")) {
//            builder.promptTokens(usage.get("prompt_tokens").asInt());
//        }
//        if (usage.has("completion_tokens")) {
//            builder.completionTokens(usage.get("completion_tokens").asInt());
//        }
//        if (usage.has("total_tokens")) {
//            builder.totalTokens(usage.get("total_tokens").asInt());
//        }
//
//        // Anthropic 字段名（与 OpenAI 字段互斥或并存时，后者覆盖前者）
//        if (usage.has("input_tokens")) {
//            builder.promptTokens(usage.get("input_tokens").asInt());
//        }
//        if (usage.has("output_tokens")) {
//            builder.completionTokens(usage.get("output_tokens").asInt());
//        }
//
//        // prompt tokens details
//        JsonNode promptDetails = usage.get("prompt_tokens_details");
//        if (promptDetails != null && !promptDetails.isNull()) {
//            TokenUsage.PromptTokensDetails.PromptTokensDetailsBuilder pdBuilder =
//                    TokenUsage.PromptTokensDetails.builder();
//            if (promptDetails.has("cached_tokens")) {
//                pdBuilder.cachedTokens(promptDetails.get("cached_tokens").asInt());
//            }
//            if (promptDetails.has("audio_tokens")) {
//                pdBuilder.audioTokens(promptDetails.get("audio_tokens").asInt());
//            }
//            if (promptDetails.has("text_tokens")) {
//                pdBuilder.textTokens(promptDetails.get("text_tokens").asInt());
//            }
//            if (promptDetails.has("image_tokens")) {
//                pdBuilder.imageTokens(promptDetails.get("image_tokens").asInt());
//            }
//            if (promptDetails.has("video_tokens")) {
//                pdBuilder.videoTokens(promptDetails.get("video_tokens").asInt());
//            }
//            builder.promptTokensDetails(pdBuilder.build());
//        }
//
//        // completion tokens details
//        JsonNode completionDetails = usage.get("completion_tokens_details");
//        if (completionDetails != null && !completionDetails.isNull()) {
//            TokenUsage.CompletionTokensDetails.CompletionTokensDetailsBuilder cdBuilder =
//                    TokenUsage.CompletionTokensDetails.builder();
//            if (completionDetails.has("audio_tokens")) {
//                cdBuilder.audioTokens(completionDetails.get("audio_tokens").asInt());
//            }
//            if (completionDetails.has("reasoning_tokens")) {
//                cdBuilder.reasoningTokens(completionDetails.get("reasoning_tokens").asInt());
//            }
//            if (completionDetails.has("text_tokens")) {
//                cdBuilder.textTokens(completionDetails.get("text_tokens").asInt());
//            }
//            builder.completionTokensDetails(cdBuilder.build());
//        }
//
//        // cache creation (Qwen)
//        JsonNode cacheCreation = usage.get("cache_creation");
//        if (cacheCreation != null && !cacheCreation.isNull()) {
//            TokenUsage.CacheCreation.CacheCreationBuilder ccBuilder =
//                    TokenUsage.CacheCreation.builder();
//            if (cacheCreation.has("cache_creation_input_tokens")) {
//                ccBuilder.cacheCreationInputTokens(cacheCreation.get("cache_creation_input_tokens").asInt());
//            }
//            if (cacheCreation.has("cache_type")) {
//                ccBuilder.cacheType(cacheCreation.get("cache_type").asText(null));
//            }
//            builder.cacheCreation(ccBuilder.build());
//        }

        return builder.build();
    }
}
