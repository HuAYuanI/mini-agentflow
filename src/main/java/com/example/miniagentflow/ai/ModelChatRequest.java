package com.example.miniagentflow.ai;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelChatRequest {

    private String conversationId;
    private String systemPrompt;
    private String userText;
    private String promptTemplate;

    @Builder.Default
    private Map<String, Object> templateVariables = new HashMap<>();
}
