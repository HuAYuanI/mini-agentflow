package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import java.util.List;

// 【中断错误处理策略】：节点执行失败时，中断整个工作流
public class InterruptNodeErrorStrategy implements NodeErrorStrategy {

    // 支持的错误策略类型
    @Override
    public ErrorStrategyEnum supportType() {
        return ErrorStrategyEnum.INTERRUPT;
    }

    // 节点执行失败时的决策：中断整个工作流
    @Override
    public NodeFailureDecision onFailure(WorkflowNode node, List<String> downstreamNodeIds) {
        return NodeFailureDecision.interrupt();
    }
}
