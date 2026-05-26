package com.windsearcher.domain;

import java.util.List;

public interface ChatMessage {

    Role role;
    String content;
    List<ContentBlock> blocks;
    List<ToolCall> toolCalls;
    String toolCallId;
    String toolName;

}
