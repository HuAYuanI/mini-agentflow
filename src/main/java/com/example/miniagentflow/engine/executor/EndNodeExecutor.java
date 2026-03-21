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
public class EndNodeExecutor extends AbstractNodeExecutor {

    private final VariableResolver variableResolver;

    public EndNodeExecutor(VariableResolver variableResolver) {
        this.variableResolver = variableResolver;
    }

    @Override
    public NodeType supportType() {
        return NodeType.END;
    }

    @Override
    protected Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node) {
        Map<String, Object> config = new HashMap<>(node.getConfig());
        config.putIfAbsent("result", "${pluginOutput}");
        config.putIfAbsent("outputKey", "finalOutput");
        Map<String, Object> resolved = variableResolver.resolveMap(config, context);
        sleepIfNeeded(resolved.get("delayMs"));

        Object result = resolved.get("result");
        String outputKey = String.valueOf(resolved.get("outputKey"));

        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, result);
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
