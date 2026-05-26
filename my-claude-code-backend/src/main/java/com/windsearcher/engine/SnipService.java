package com.windsearcher.engine;


import com.windsearcher.domain.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SnipService {

    /**
     * 遍历消息列表，对所有工具结果应用 Snip。
     *
     * @param messages    消息列表
     * @param budgetChars 每条工具结果的字符预算
     * @return 截断后的消息列表 (新 List，不修改原列表)
     */
    public List<ChatMessage> snipToolResults(List<ChatMessage> messages, int budgetChars) {
        List<ChatMessage> result = new ArrayList<>(messages.size());
//        for (ChatMessage msg : messages) {
//            if (msg instanceof ChatMessage.UserMessage user
//                    && user.toolUseResult() != null
//                    && user.toolUseResult().length() > budgetChars) {
//                String snipped = snipIfNeeded(user.toolUseResult(), budgetChars);
//                result.add(new ChatMessage.UserMessage(
//                        user.uuid(), user.timestamp(), user.content(),
//                        snipped, user.sourceToolAssistantUUID()));
//            } else {
//                result.add(msg);
//            }
//        }
        return result;
    }

}
