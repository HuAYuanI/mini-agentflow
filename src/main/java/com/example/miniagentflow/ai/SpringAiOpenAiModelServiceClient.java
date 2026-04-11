package com.example.miniagentflow.ai;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.util.StringUtils;

public class SpringAiOpenAiModelServiceClient implements ModelServiceClient {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final PromptTemplateService promptTemplateService;

    public SpringAiOpenAiModelServiceClient(ChatClient.Builder chatClientBuilder,
            ChatMemory chatMemory,
            PromptTemplateService promptTemplateService) {
        this.chatMemory = chatMemory;
        this.promptTemplateService = promptTemplateService;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Override
    public ModelChatResponse complete(ModelChatRequest request) {
        String conversationId = normalizeConversationId(request.getConversationId());
        PromptTemplateService.RenderedPrompts renderedPrompts = promptTemplateService.render(request);
        String content = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(renderedPrompts.systemPrompt())
                .user(renderedPrompts.userPrompt())
                .call()
                .content();
        List<Message> messages = chatMemory.get(conversationId);

        return ModelChatResponse.builder()
                .conversationId(conversationId)
                .provider("SPRING_AI_OPENAI")
                .mock(false)
                .memorySize(messages == null ? 0 : messages.size())
                .content(content)
                .build();
    }

    private String normalizeConversationId(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            return conversationId;
        }
        return ChatMemory.DEFAULT_CONVERSATION_ID;
    }
}
