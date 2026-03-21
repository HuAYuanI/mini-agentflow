package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeRunResult;
import com.example.miniagentflow.domain.NodeRunStatus;
import com.example.miniagentflow.domain.WorkflowNode;
import java.util.Map;

// 【抽象节点执行器】：定义节点执行的模板方法，提供模板方法模式
public abstract class AbstractNodeExecutor implements NodeExecutor {

    @Override
    public final NodeRunResult execute(NodeExecutionContext context, WorkflowNode node) {
        try {
            // 执行前的预处理（比如打印日志，提取特定变量，子类可选重写）
            beforeExecute(context, node);

            // 执行节点逻辑（真正干活，抽象方法，调用的是子类重写的 doExecute，具体是什么节点，自己去写逻辑）
            Map<String, Object> output = doExecute(context, node);

            // 执行后置操作（由父类统一定义成功返回的数据格式）
            NodeRunResult result = NodeRunResult.success(node.getId(), output);
            afterExecute(context, node, result);
            return result;
        } catch (Exception ex) {
            // 异常处理
            return NodeRunResult.builder()
                    .nodeId(node.getId())
                    .status(NodeRunStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    // 【执行前置操作】：节点执行前的准备工作，可由子类覆盖
    protected void beforeExecute(NodeExecutionContext context, WorkflowNode node) {
    }

    // 【执行节点逻辑】：节点的核心业务逻辑，必须由子类实现
    protected abstract Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node);

    // 【执行后置操作】：节点执行后的清理工作，可由子类覆盖
    protected void afterExecute(NodeExecutionContext context, WorkflowNode node, NodeRunResult result) {
    }
}
