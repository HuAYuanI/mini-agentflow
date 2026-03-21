package com.example.miniagentflow.engine.executor;

import com.example.miniagentflow.domain.NodeType;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.engine.AbstractNodeExecutor;
import com.example.miniagentflow.engine.NodeExecutionContext;
import com.example.miniagentflow.engine.VariableResolver;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PluginNodeExecutor extends AbstractNodeExecutor {

    private final VariableResolver variableResolver;

    public PluginNodeExecutor(VariableResolver variableResolver) {
        this.variableResolver = variableResolver;
    }

    @Override
    public NodeType supportType() {
        return NodeType.PLUGIN;
    }

    @Override
    protected Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node) {
        Map<String, Object> config = new HashMap<>(node.getConfig());
        config.putIfAbsent("text", "${llmOutput}");
        config.putIfAbsent("outputKey", "pluginOutput");
        Map<String, Object> resolved = variableResolver.resolveMap(config, context);
        sleepIfNeeded(resolved.get("delayMs"));

        String text = String.valueOf(resolved.get("text"));
        String outputKey = String.valueOf(resolved.get("outputKey"));

        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, "PLUGIN_OK: " + text);
        return output;
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
