package com.example.miniagentflow.ai;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptTemplateService {

    private static final String DEFAULT_USER_PROMPT_TEMPLATE = "{input}";

    private final AgentflowAiProperties agentflowAiProperties;

    public PromptTemplateService(AgentflowAiProperties agentflowAiProperties) {
        this.agentflowAiProperties = agentflowAiProperties;
    }

    public RenderedPrompts render(ModelChatRequest request) {
        Map<String, Object> variables = new HashMap<>();
        if (request.getTemplateVariables() != null) {
            variables.putAll(request.getTemplateVariables());
        }
        variables.putIfAbsent("input", request.getUserText() == null ? "" : request.getUserText());

        String userPrompt = new PromptTemplate(resolveUserPromptTemplate(request)).render(variables);
        String systemPrompt = new SystemPromptTemplate(resolveSystemPromptTemplate(request)).render(variables);

        return new RenderedPrompts(userPrompt, systemPrompt, variables);
    }

    private String resolveUserPromptTemplate(ModelChatRequest request) {
        if (StringUtils.hasText(request.getPromptTemplate())) {
            return request.getPromptTemplate();
        }
        return DEFAULT_USER_PROMPT_TEMPLATE;
    }

    private String resolveSystemPromptTemplate(ModelChatRequest request) {
        if (StringUtils.hasText(request.getSystemPrompt())) {
            return request.getSystemPrompt();
        }
        return agentflowAiProperties.getDefaultSystemPrompt();
    }

    public record RenderedPrompts(String userPrompt, String systemPrompt, Map<String, Object> variables) {
    }
}
