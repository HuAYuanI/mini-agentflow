package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.EngineMode;
import com.example.miniagentflow.domain.NodeRunResult;
import com.example.miniagentflow.domain.NodeRunStatus;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowEventType;
import com.example.miniagentflow.domain.WorkflowEdge;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.error.NodeErrorStrategySelector;
import com.example.miniagentflow.engine.error.NodeFailureDecision;
import com.example.miniagentflow.engine.event.CollectingWorkflowEventListener;
import com.example.miniagentflow.engine.event.CompositeWorkflowEventListener;
import com.example.miniagentflow.engine.event.NoopWorkflowEventListener;
import com.example.miniagentflow.engine.event.WorkflowEventListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SerialWorkflowEngine {

    private final WorkflowValidator workflowValidator;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final NodeErrorStrategySelector nodeErrorStrategySelector;

    @Autowired
    public SerialWorkflowEngine(WorkflowValidator workflowValidator, NodeExecutorRegistry nodeExecutorRegistry) {
        this(workflowValidator, nodeExecutorRegistry, new NodeErrorStrategySelector());
    }

    public SerialWorkflowEngine(WorkflowValidator workflowValidator,
            NodeExecutorRegistry nodeExecutorRegistry,
            NodeErrorStrategySelector nodeErrorStrategySelector) {
        this.workflowValidator = workflowValidator;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.nodeErrorStrategySelector = nodeErrorStrategySelector;
    }

    public WorkflowRunResult run(WorkflowDefinition workflow, Map<String, Object> inputs) {
        return run(workflow, inputs, NoopWorkflowEventListener.INSTANCE);
    }

    public WorkflowRunResult run(WorkflowDefinition workflow,
            Map<String, Object> inputs,
            WorkflowEventListener eventListener) {
        List<String> topologicalOrder = workflowValidator.validateAndSort(workflow, inputs);
        Map<String, WorkflowNode> nodeMap = buildNodeMap(workflow);
        Map<String, List<String>> adjacency = buildAdjacency(workflow, nodeMap.keySet());
        Map<String, AtomicInteger> remainingDeps = buildRemainingDeps(workflow, nodeMap.keySet());

        CollectingWorkflowEventListener collector = new CollectingWorkflowEventListener();
        WorkflowEventListener compositeListener = CompositeWorkflowEventListener.of(collector, eventListener);
        WorkflowThreadContext threadContext = WorkflowThreadContextHolder.init(EngineMode.SERIAL);

        try {
            NodeExecutionContext context = new NodeExecutionContext(inputs, threadContext, compositeListener);
            context.putVariable("executionId", threadContext.getExecutionId());
            context.putVariable("engineMode", threadContext.getEngineMode());
            context.putVariable("startedAtMillis", threadContext.getStartedAtMillis());
            context.publishWorkflowEvent(
                    WorkflowEventType.WORKFLOW_STARTED,
                    "RUNNING",
                    "Workflow execution started",
                    Map.of("nodeCount", nodeMap.size()));

            Queue<String> readyQueue = new ArrayDeque<>();
            Map<String, NodeRunResult> nodeResultMap = new HashMap<>();
            boolean interrupted = false;
            boolean handledFailure = false;

            for (String nodeId : nodeMap.keySet()) {
                if (remainingDeps.get(nodeId).get() == 0) {
                    readyQueue.offer(nodeId);
                }
            }

            while (!readyQueue.isEmpty()) {
                String nodeId = readyQueue.poll();
                WorkflowNode node = nodeMap.get(nodeId);
                NodeRunResult result = nodeExecutorRegistry.getExecutor(node.getType()).execute(context, node);
                nodeResultMap.put(nodeId, result);
                context.putNodeOutput(nodeId, result.getOutput());

                if (result.getStatus() == NodeRunStatus.FAILED) {
                    handledFailure = true;
                    context.putVariable("lastErrorNode", nodeId);
                    context.putVariable("lastErrorMessage", result.getErrorMessage());
                    NodeFailureDecision decision = nodeErrorStrategySelector.decide(node, adjacency.get(nodeId));
                    if (decision.isInterruptWorkflow()) {
                        interrupted = true;
                        break;
                    }
                    releaseNextNodes(decision.getNextNodeIds(), remainingDeps, readyQueue);
                    continue;
                }

                if (result.getOutput() != null) {
                    result.getOutput().forEach(context::putVariable);
                }
                releaseNextNodes(adjacency.get(nodeId), remainingDeps, readyQueue);
            }

            List<NodeRunResult> orderedResults = new ArrayList<>();
            for (String nodeId : topologicalOrder) {
                NodeRunResult result = nodeResultMap.get(nodeId);
                if (result != null) {
                    orderedResults.add(result);
                }
            }

            String status = interrupted ? "FAILED" : handledFailure ? "PARTIAL_SUCCESS" : "SUCCESS";
            context.publishWorkflowEvent(
                    WorkflowEventType.WORKFLOW_COMPLETED,
                    status,
                    "Workflow execution completed",
                    Map.of("executedNodeCount", orderedResults.size()));

            return WorkflowRunResult.builder()
                    .status(status)
                    .nodeResults(orderedResults)
                    .events(collector.snapshot())
                    .contextSnapshot(context.snapshotVariables())
                    .build();
        } finally {
            WorkflowThreadContextHolder.clear();
        }
    }

    private void releaseNextNodes(List<String> nextNodes,
            Map<String, AtomicInteger> remainingDeps,
            Queue<String> readyQueue) {
        if (nextNodes == null) {
            return;
        }
        for (String next : nextNodes) {
            if (remainingDeps.get(next).decrementAndGet() == 0) {
                readyQueue.offer(next);
            }
        }
    }

    private Map<String, WorkflowNode> buildNodeMap(WorkflowDefinition workflow) {
        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNode node : workflow.getNodes()) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    private Map<String, List<String>> buildAdjacency(WorkflowDefinition workflow, Set<String> nodeIds) {
        Map<String, List<String>> adjacency = new HashMap<>();
        for (String nodeId : nodeIds) {
            adjacency.put(nodeId, new ArrayList<>());
        }
        for (WorkflowEdge edge : workflow.getEdges()) {
            adjacency.get(edge.getFrom()).add(edge.getTo());
        }
        return adjacency;
    }

    private Map<String, AtomicInteger> buildRemainingDeps(WorkflowDefinition workflow, Set<String> nodeIds) {
        Map<String, AtomicInteger> remainingDeps = new HashMap<>();
        for (String nodeId : nodeIds) {
            remainingDeps.put(nodeId, new AtomicInteger(0));
        }
        for (WorkflowEdge edge : workflow.getEdges()) {
            remainingDeps.get(edge.getTo()).incrementAndGet();
        }
        return remainingDeps;
    }
}
