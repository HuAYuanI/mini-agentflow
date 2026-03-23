package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.exception.WorkflowValidationException;
import java.util.List;

public class ErrorBranchNodeErrorStrategy implements NodeErrorStrategy {

    @Override
    public ErrorStrategyEnum supportType() {
        return ErrorStrategyEnum.ERROR_BRANCH;
    }

    @Override
    public NodeFailureDecision onFailure(WorkflowNode node, List<String> downstreamNodeIds) {
        Object errorNextObj = node.getConfig().get("errorNext");
        if (!(errorNextObj instanceof String errorNext) || errorNext.isBlank()) {
            throw new WorkflowValidationException("Node " + node.getId() + " missing errorNext for ERROR_BRANCH");
        }
        if (downstreamNodeIds == null || !downstreamNodeIds.contains(errorNext)) {
            throw new WorkflowValidationException(
                    "Node " + node.getId() + " errorNext must be a direct downstream node: " + errorNext);
        }
        return NodeFailureDecision.continueWith(List.of(errorNext));
    }
}
