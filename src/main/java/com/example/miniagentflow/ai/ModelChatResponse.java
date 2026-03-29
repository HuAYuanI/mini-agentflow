package com.example.miniagentflow.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelChatResponse {

    private String conversationId;
    private String provider;
    private boolean mock;
    private int memorySize;
    private String content;
}
