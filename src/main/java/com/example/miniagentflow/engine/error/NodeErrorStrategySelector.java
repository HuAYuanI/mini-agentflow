package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.exception.WorkflowValidationException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// 【节点错误策略选择器】：根据节点错误策略类型获取对应的错误处理策略
public class NodeErrorStrategySelector {

    // 【策略映射】：存储错误策略类型到错误处理策略的映射
    private final Map<ErrorStrategyEnum, NodeErrorStrategy> strategyMap = new EnumMap<>(ErrorStrategyEnum.class);

    public NodeErrorStrategySelector() {
        this(List.of(
                new InterruptNodeErrorStrategy(), // 中断策略
                new ContinueNodeErrorStrategy(), // 继续策略
                new ErrorBranchNodeErrorStrategy() // 错误分支策略
        ));
    }

    // 构造函数：初始化时将所有错误处理策略注册到映射中
    public NodeErrorStrategySelector(List<NodeErrorStrategy> strategies) {
        for (NodeErrorStrategy strategy : strategies) {
            strategyMap.put(strategy.supportType(), strategy);
        }
    }

    // 【决策】：根据节点错误策略类型获取对应的错误处理策略
    public NodeFailureDecision decide(WorkflowNode node, List<String> downstreamNodeIds) {
        ErrorStrategyEnum strategyType = parseStrategy(node.getConfig().get("errorStrategy"));
        NodeErrorStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new WorkflowValidationException("Unsupported error strategy: " + strategyType);
        }
        return strategy.onFailure(node, downstreamNodeIds);
    }

    // 【解析】：解析错误策略类型
    public ErrorStrategyEnum parseStrategy(Object strategyObj) {
        if (strategyObj == null) {
            return ErrorStrategyEnum.INTERRUPT;
        }
        String raw = String.valueOf(strategyObj).trim();
        if (raw.isEmpty()) {
            return ErrorStrategyEnum.INTERRUPT;
        }
        try {
            return ErrorStrategyEnum.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new WorkflowValidationException("Unknown errorStrategy: " + raw);
        }
    }
}
