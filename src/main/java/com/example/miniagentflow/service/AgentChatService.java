package com.example.miniagentflow.service;

import com.example.miniagentflow.ai.ModelChatRequest;
import com.example.miniagentflow.ai.ModelChatResponse;
import com.example.miniagentflow.ai.ModelServiceClient;
import com.example.miniagentflow.api.dto.ChatCompletionRequest;
import com.example.miniagentflow.api.dto.ChatCompletionResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentChatService {

    private final ModelServiceClient modelServiceClient;

    public AgentChatService(ModelServiceClient modelServiceClient) {
        this.modelServiceClient = modelServiceClient;
    }

    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        ModelChatResponse response = modelServiceClient.complete(ModelChatRequest.builder()
                .conversationId(conversationId)
                .systemPrompt(request.getSystemPrompt())
                .userText(request.getUserInput())
                .promptTemplate(request.getPromptTemplate())
                .templateVariables(request.getTemplateVariables())
                .build());

        return ChatCompletionResponse.builder()
                .conversationId(response.getConversationId())
                .provider(response.getProvider())
                .mock(response.isMock())
                .memorySize(response.getMemorySize())
                .content(response.getContent())
                .build();
    }

    private String resolveConversationId(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            return conversationId;
        }
        return UUID.randomUUID().toString();
    }
}
