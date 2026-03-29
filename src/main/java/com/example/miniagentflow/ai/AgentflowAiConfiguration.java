package com.example.miniagentflow.ai;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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
}
