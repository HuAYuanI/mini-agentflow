package com.example.miniagentflow.engine.error;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.exception.WorkflowValidationException;
import java.util.List;

// 【错误分支错误处理策略】：节点执行失败时，执行错误分支
public class ErrorBranchNodeErrorStrategy implements NodeErrorStrategy {

    // 支持的错误策略类型
    @Override
    public ErrorStrategyEnum supportType() {
        return ErrorStrategyEnum.ERROR_BRANCH;
    }

    // 节点执行失败时的决策：执行错误分支
    @Override
    public NodeFailureDecision onFailure(WorkflowNode node, List<String> downstreamNodeIds) {
        // 获取错误分支节点ID
        Object errorNextObj = node.getConfig().get("errorNext");
        // 验证错误分支节点ID
        if (!(errorNextObj instanceof String errorNext) || errorNext.isBlank()) {
            throw new WorkflowValidationException("Node " + node.getId() + " missing errorNext for ERROR_BRANCH");
        }
        // 验证错误分支节点ID是否是直接下游节点
        if (downstreamNodeIds == null || !downstreamNodeIds.contains(errorNext)) {
            throw new WorkflowValidationException(
                    "Node " + node.getId() + " errorNext must be a direct downstream node: " + errorNext);
        }
        // 返回错误分支节点ID
        return NodeFailureDecision.continueWith(List.of(errorNext));
    }
}
