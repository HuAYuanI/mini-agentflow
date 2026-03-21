package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeType;
import com.example.miniagentflow.exception.WorkflowValidationException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

// 【节点执行器注册表】：根据节点类型获取对应的执行器
@Component
public class NodeExecutorRegistry {

    // 【执行器映射】：存储节点类型到执行器的映射，EnumMap保证线程安全
    private final Map<NodeType, NodeExecutor> executorMap = new EnumMap<>(NodeType.class);

    // 构造函数：初始化时将所有执行器注册到映射中
    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        for (NodeExecutor executor : executors) {
            executorMap.put(executor.supportType(), executor);
        }
    }

    // 【获取执行器】：根据节点类型获取对应的执行器
    public NodeExecutor getExecutor(NodeType type) {
        NodeExecutor executor = executorMap.get(type);
        if (executor == null) {
            throw new WorkflowValidationException("No executor found for type: " + type);
        }
        return executor;
    }
}
