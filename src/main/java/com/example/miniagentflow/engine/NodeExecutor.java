package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeRunResult;
import com.example.miniagentflow.domain.NodeType;
import com.example.miniagentflow.domain.WorkflowNode;

public interface NodeExecutor {

    NodeType supportType();

    NodeRunResult execute(NodeExecutionContext context, WorkflowNode node);
}
