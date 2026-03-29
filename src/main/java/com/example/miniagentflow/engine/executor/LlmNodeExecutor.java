package com.example.miniagentflow.engine.executor;

import com.example.miniagentflow.ai.ModelChatRequest;
import com.example.miniagentflow.ai.ModelChatResponse;
import com.example.miniagentflow.ai.ModelServiceClient;
import com.example.miniagentflow.domain.NodeType;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.engine.AbstractNodeExecutor;
import com.example.miniagentflow.engine.NodeExecutionContext;
import com.example.miniagentflow.engine.VariableResolver;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LlmNodeExecutor extends AbstractNodeExecutor {

    private final VariableResolver variableResolver;
    private final ModelServiceClient modelServiceClient;

    public LlmNodeExecutor(VariableResolver variableResolver, ModelServiceClient modelServiceClient) {
        this.variableResolver = variableResolver;
        this.modelServiceClient = modelServiceClient;
    }

    @Override
    public NodeType supportType() {
        return NodeType.LLM;
    }

    @Override
    protected Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node) {
        Map<String, Object> config = new HashMap<>(node.getConfig());
        config.putIfAbsent("prompt", "${startOutput}");
        config.putIfAbsent("outputKey", "llmOutput");
        Map<String, Object> resolved = variableResolver.resolveMap(config, context);
        sleepIfNeeded(resolved.get("delayMs"));

        String outputKey = String.valueOf(resolved.get("outputKey"));
        String prompt = String.valueOf(resolved.get("prompt"));
        ModelChatResponse chatResponse = modelServiceClient.complete(ModelChatRequest.builder()
                .conversationId(resolveConversationId(context, node, resolved))
                .systemPrompt(stringValue(resolved.get("systemPrompt")))
                .userText(prompt)
                .promptTemplate(stringValue(resolved.get("promptTemplate")))
                .templateVariables(resolveTemplateVariables(resolved.get("templateVariables")))
                .build());

        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, chatResponse.getContent());
        return output;
    }

    private String resolveConversationId(NodeExecutionContext context, WorkflowNode node, Map<String, Object> resolved) {
        String configuredConversationId = stringValue(resolved.get("conversationId"));
        if (StringUtils.hasText(configuredConversationId)) {
            return configuredConversationId;
        }
        if (StringUtils.hasText(context.getExecutionId())) {
            return context.getExecutionId() + ":" + node.getId();
        }
        return "workflow:" + node.getId();
    }

    private Map<String, Object> resolveTemplateVariables(Object templateVariablesValue) {
        if (!(templateVariablesValue instanceof Map<?, ?> rawMap)) {
            return new HashMap<>();
        }
        Map<String, Object> templateVariables = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                templateVariables.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return templateVariables;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private void sleepIfNeeded(Object delayValue) {
        if (delayValue == null) {
            return;
        }
        long delayMs = Long.parseLong(String.valueOf(delayValue));
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
