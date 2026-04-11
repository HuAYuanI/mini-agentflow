package com.example.miniagentflow.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentflowAiProperties.class)
public class AgentflowAiConfiguration {

    @Bean
    public ChatMemory chatMemory(AgentflowAiProperties agentflowAiProperties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(agentflowAiProperties.getMemoryWindow())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "miniagentflow.ai", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
    public ModelServiceClient mockModelServiceClient(AgentflowAiProperties agentflowAiProperties,
            PromptTemplateService promptTemplateService) {
        return new MockModelServiceClient(agentflowAiProperties, promptTemplateService);
    }

    @Bean
    @ConditionalOnBean(ChatClient.Builder.class)
    @ConditionalOnProperty(prefix = "miniagentflow.ai", name = "mock-enabled", havingValue = "false")
    public ModelServiceClient springAiOpenAiModelServiceClient(ChatClient.Builder chatClientBuilder,
            ChatMemory chatMemory,
            PromptTemplateService promptTemplateService) {
        return new SpringAiOpenAiModelServiceClient(chatClientBuilder, chatMemory, promptTemplateService);
    }
}
