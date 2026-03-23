package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import java.util.List;

public class InterruptNodeErrorStrategy implements NodeErrorStrategy {

    @Override
    public ErrorStrategyEnum supportType() {
        return ErrorStrategyEnum.INTERRUPT;
    }

    @Override
    public NodeFailureDecision onFailure(WorkflowNode node, List<String> downstreamNodeIds) {
        return NodeFailureDecision.interrupt();
    }
}
