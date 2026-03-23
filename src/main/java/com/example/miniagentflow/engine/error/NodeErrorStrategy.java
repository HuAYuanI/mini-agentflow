package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import java.util.List;

public interface NodeErrorStrategy {

    ErrorStrategyEnum supportType();

    NodeFailureDecision onFailure(WorkflowNode node, List<String> downstreamNodeIds);
}
