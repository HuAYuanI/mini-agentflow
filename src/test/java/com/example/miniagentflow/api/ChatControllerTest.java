package com.example.miniagentflow.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.miniagentflow.api.dto.ChatCompletionRequest;
import com.example.miniagentflow.api.dto.ChatCompletionResponse;
import com.example.miniagentflow.service.AgentChatService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ChatControllerTest {

    @Test
    void shouldDelegateChatCompletionRequest() {
        AgentChatService agentChatService = mock(AgentChatService.class);
        ChatController chatController = new ChatController(agentChatService);
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .userInput("你好")
                .build();
        ChatCompletionResponse expected = ChatCompletionResponse.builder()
                .conversationId("conv-1")
                .content("answer")
                .build();

        when(agentChatService.complete(request)).thenReturn(expected);

        ChatCompletionResponse actual = chatController.complete(request);
        Assertions.assertSame(expected, actual);
    }
}
