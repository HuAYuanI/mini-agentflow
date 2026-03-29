package com.example.miniagentflow.service;

import com.example.miniagentflow.ai.ModelChatRequest;
import com.example.miniagentflow.ai.ModelChatResponse;
import com.example.miniagentflow.ai.ModelServiceClient;
import com.example.miniagentflow.api.dto.ChatCompletionRequest;
import com.example.miniagentflow.api.dto.ChatCompletionResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentChatServiceTest {

    @Test
    void shouldGenerateConversationIdWhenMissing() {
        AtomicReference<ModelChatRequest> capturedRequest = new AtomicReference<>();
        ModelServiceClient modelServiceClient = request -> {
            capturedRequest.set(request);
            return ModelChatResponse.builder()
                    .conversationId(request.getConversationId())
                    .provider("MOCK")
                    .mock(true)
                    .memorySize(2)
                    .content("ok")
                    .build();
        };
        AgentChatService agentChatService = new AgentChatService(modelServiceClient);

        ChatCompletionResponse response = agentChatService.complete(ChatCompletionRequest.builder()
                .userInput("你好")
                .build());

        Assertions.assertNotNull(capturedRequest.get().getConversationId());
        Assertions.assertFalse(capturedRequest.get().getConversationId().isBlank());
        Assertions.assertEquals(capturedRequest.get().getConversationId(), response.getConversationId());
        Assertions.assertEquals("ok", response.getContent());
    }
}
