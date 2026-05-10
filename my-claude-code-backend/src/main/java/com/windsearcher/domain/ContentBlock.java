package com.windsearcher.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentBlock {

    /**
     * 块类型：text / image / tool_use / tool_result / thinking等
     */
    private String type;

    /**
     * 文本内容（type=text/thinking时使用）
     */
    private String text;

    private String imageUrl;

    private String toolUseId;

    private String toolName;

    private String toolInput;

    private String toolResult;

    private Boolean isError;

}
