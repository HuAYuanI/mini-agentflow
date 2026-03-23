package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.exception.WorkflowValidationException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NodeErrorStrategySelector {

    private final Map<ErrorStrategyEnum, NodeErrorStrategy> strategyMap = new EnumMap<>(ErrorStrategyEnum.class);

    public NodeErrorStrategySelector() {
        this(List.of(
                new InterruptNodeErrorStrategy(),
                new ContinueNodeErrorStrategy(),
                new ErrorBranchNodeErrorStrategy()
        ));
    }

    public NodeErrorStrategySelector(List<NodeErrorStrategy> strategies) {
        for (NodeErrorStrategy strategy : strategies) {
            strategyMap.put(strategy.supportType(), strategy);
        }
    }

    public NodeFailureDecision decide(WorkflowNode node, List<String> downstreamNodeIds) {
        ErrorStrategyEnum strategyType = parseStrategy(node.getConfig().get("errorStrategy"));
        NodeErrorStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new WorkflowValidationException("Unsupported error strategy: " + strategyType);
        }
        return strategy.onFailure(node, downstreamNodeIds);
    }

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
