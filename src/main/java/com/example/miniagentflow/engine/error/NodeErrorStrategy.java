package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import java.util.List;

// 【节点错误处理策略】：节点执行失败时的处理策略
public interface NodeErrorStrategy {

    // 支持的错误策略类型
    ErrorStrategyEnum supportType();

    // 节点执行失败时的决策
    NodeFailureDecision onFailure(WorkflowNode node, List<String> downstreamNodeIds);
}
