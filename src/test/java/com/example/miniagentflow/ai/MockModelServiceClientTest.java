package com.example.miniagentflow.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MockModelServiceClientTest {

    @Test
    void shouldKeepConversationWithinSlidingWindow() {
        AgentflowAiProperties properties = new AgentflowAiProperties();
        properties.setMemoryWindow(3);
        properties.setDefaultSystemPrompt("你是测试助手");
        MockModelServiceClient modelServiceClient =
                new MockModelServiceClient(properties, new PromptTemplateService(properties));

        ModelChatResponse firstResponse = modelServiceClient.complete(ModelChatRequest.builder()
                .conversationId("conv-1")
                .userText("第一句")
                .build());
        ModelChatResponse secondResponse = modelServiceClient.complete(ModelChatRequest.builder()
                .conversationId("conv-1")
                .userText("第二句")
                .build());
        ModelChatResponse thirdResponse = modelServiceClient.complete(ModelChatRequest.builder()
                .conversationId("conv-1")
                .userText("第三句")
                .build());

        Assertions.assertEquals(2, firstResponse.getMemorySize());
        Assertions.assertEquals(3, secondResponse.getMemorySize());
        Assertions.assertEquals(3, thirdResponse.getMemorySize());
        Assertions.assertTrue(thirdResponse.getContent().contains("MOCK_RESPONSE"));
    }
}
