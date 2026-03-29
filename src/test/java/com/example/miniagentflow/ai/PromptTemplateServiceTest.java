package com.example.miniagentflow.ai;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PromptTemplateServiceTest {

    @Test
    void shouldRenderUserAndSystemPromptTemplates() {
        AgentflowAiProperties properties = new AgentflowAiProperties();
        properties.setDefaultSystemPrompt("你是{role}");
        PromptTemplateService promptTemplateService = new PromptTemplateService(properties);

        PromptTemplateService.RenderedPrompts renderedPrompts = promptTemplateService.render(ModelChatRequest.builder()
                .userText("你好")
                .promptTemplate("请把{input}整理成{scene}风格")
                .templateVariables(Map.of("scene", "简历项目", "role", "面试官"))
                .build());

        Assertions.assertEquals("请把你好整理成简历项目风格", renderedPrompts.userPrompt());
        Assertions.assertEquals("你是面试官", renderedPrompts.systemPrompt());
    }
}
