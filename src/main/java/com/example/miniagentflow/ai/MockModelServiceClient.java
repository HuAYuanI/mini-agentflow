package com.example.miniagentflow.ai;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnMissingBean(ModelServiceClient.class)
public class MockModelServiceClient implements ModelServiceClient {

    private final AgentflowAiProperties agentflowAiProperties;
    private final PromptTemplateService promptTemplateService;
    private final Map<String, Deque<String>> conversationHistory = new ConcurrentHashMap<>();

    public MockModelServiceClient(AgentflowAiProperties agentflowAiProperties,
            PromptTemplateService promptTemplateService) {
        this.agentflowAiProperties = agentflowAiProperties;
        this.promptTemplateService = promptTemplateService;
    }

    @Override
    public ModelChatResponse complete(ModelChatRequest request) {
        String conversationId = normalizeConversationId(request.getConversationId());
        PromptTemplateService.RenderedPrompts renderedPrompts = promptTemplateService.render(request);
        Deque<String> history = conversationHistory.computeIfAbsent(conversationId, key -> new ConcurrentLinkedDeque<>());
        String content;
        int memorySize;

        synchronized (history) {
            history.addLast("USER:" + renderedPrompts.userPrompt());
            trim(history);
            content = buildMockContent(renderedPrompts);
            history.addLast("ASSISTANT:" + content);
            trim(history);
            memorySize = history.size();
        }

        return ModelChatResponse.builder()
                .conversationId(conversationId)
                .provider("MOCK")
                .mock(true)
                .memorySize(memorySize)
                .content(content)
                .build();
    }

    private String normalizeConversationId(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            return conversationId;
        }
        return "mock-default";
    }

    private void trim(Deque<String> history) {
        while (history.size() > agentflowAiProperties.getMemoryWindow()) {
            history.pollFirst();
        }
    }

    private String buildMockContent(PromptTemplateService.RenderedPrompts renderedPrompts) {
        String systemPrompt = simplify(renderedPrompts.systemPrompt());
        if (StringUtils.hasText(systemPrompt)) {
            return "MOCK_RESPONSE[" + systemPrompt + "]: " + renderedPrompts.userPrompt();
        }
        return "MOCK_RESPONSE: " + renderedPrompts.userPrompt();
    }

    private String simplify(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 18) {
            return normalized;
        }
        return normalized.substring(0, 18) + "...";
    }
}
