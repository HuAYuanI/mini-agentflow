package com.example.miniagentflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResponse {

    private String conversationId;
    private String provider;
    private boolean mock;
    private int memorySize;
    private String content;
}
