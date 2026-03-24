package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import java.util.List;

// 【继续错误处理策略】：节点执行失败时，继续执行后续节点
public class ContinueNodeErrorStrategy implements NodeErrorStrategy {

    // 支持的错误策略类型
    @Override
    public ErrorStrategyEnum supportType() {
        return ErrorStrategyEnum.CONTINUE;
    }

    // 节点执行失败时的决策：继续执行后续节点
    @Override
    public NodeFailureDecision onFailure(WorkflowNode node, List<String> downstreamNodeIds) {
        return NodeFailureDecision.continueWith(downstreamNodeIds);
    }
}
