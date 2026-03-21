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
public class StartNodeExecutor extends AbstractNodeExecutor {

    private final VariableResolver variableResolver;

    public StartNodeExecutor(VariableResolver variableResolver) {
        this.variableResolver = variableResolver;
    }

    @Override
    public NodeType supportType() {
        return NodeType.START;
    }

    @Override
    protected Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node) {
        // 【1. 准备配置】（用户在 JSON 里传入的配置）
        Map<String, Object> config = new HashMap<>(node.getConfig());
        // 设置默认值（如果用户没传的话）
        config.putIfAbsent("inputKey", "input");
        config.putIfAbsent("outputKey", "startOutput");
        // 【2. 解析配置】（把 ${xxx} 替换成真实值）
        Map<String, Object> resolved = variableResolver.resolveMap(config, context);

        // 【3. 提取参数】
        String inputKey = String.valueOf(resolved.get("inputKey"));
        String outputKey = String.valueOf(resolved.get("outputKey"));
        // 获取输入（如果没传，就是 null）
        Object input = context.getVariable(inputKey);
        // 如果用户在 JSON 里写了 value，就用它；否则用 input
        Object value = resolved.containsKey("value") ? resolved.get("value") : input;

        // 【4. 准备输出】
        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, value == null ? "" : value);
        return output;
    }
}
