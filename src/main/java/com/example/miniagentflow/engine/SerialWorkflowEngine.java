package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeRunResult;
import com.example.miniagentflow.domain.NodeRunStatus;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.domain.WorkflowRunResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

// 【串行工作流引擎】：按照拓扑排序的顺序串行执行节点
@Component
public class SerialWorkflowEngine {

    // 【工作流验证器】：验证工作流的正确性并排序
    private final WorkflowValidator workflowValidator;
    // 【节点执行器注册表】：根据节点类型获取对应的执行器
    private final NodeExecutorRegistry nodeExecutorRegistry;

    public SerialWorkflowEngine(WorkflowValidator workflowValidator, NodeExecutorRegistry nodeExecutorRegistry) {
        this.workflowValidator = workflowValidator;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    // 【执行工作流】：按照拓扑排序的顺序串行执行节点
    public WorkflowRunResult run(WorkflowDefinition workflow, Map<String, Object> inputs) {
        // ===== 第一步：校验 + 排序 =====
        List<String> order = workflowValidator.validateAndSort(workflow);
        // ===== 第二步：准备执行环境 =====
        // 创建节点映射
        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNode node : workflow.getNodes()) {
            nodeMap.put(node.getId(), node);
        }

        // ===== 第三步：执行 =====
        // 创建执行上下文
        NodeExecutionContext context = new NodeExecutionContext(inputs);
        // 存储每个节点的执行结果，最终一起返回给前端
        List<NodeRunResult> nodeResults = new ArrayList<>();

        // 遍历执行节点
        for (String nodeId : order) {
            WorkflowNode node = nodeMap.get(nodeId);
            // 获取节点执行器
            NodeExecutor executor = nodeExecutorRegistry.getExecutor(node.getType());
            // 执行节点
            NodeRunResult result = executor.execute(context, node);
            // 存储节点执行结果
            nodeResults.add(result);
            // 将节点输出存入上下文
            context.putNodeOutput(nodeId, result.getOutput());
            // ===== 失败熔断 =====
            if (result.getStatus() == NodeRunStatus.FAILED) {
                // 任何一个节点失败了，整条流水线立即终止，不再往后走
                return WorkflowRunResult.builder()
                        .status("FAILED")
                        .nodeResults(nodeResults)
                        .contextSnapshot(context.getVariables())
                        .build();
            }
            // ===== 变量注入 =====
            // 将节点输出存入上下文
            if (result.getOutput() != null) {
                result.getOutput().forEach(context::putVariable);
            }
        }

        // 返回执行结果
        return WorkflowRunResult.builder()
                .status("SUCCESS")
                .nodeResults(nodeResults)
                .contextSnapshot(context.getVariables())
                .build();
    }
}
