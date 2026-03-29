package com.example.miniagentflow.api.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ChatCompletionRequest {

    private String conversationId;
    private String systemPrompt;
    private String promptTemplate;

    @NotBlank
    private String userInput;

    @Builder.Default
    private Map<String, Object> templateVariables = new HashMap<>();
}
