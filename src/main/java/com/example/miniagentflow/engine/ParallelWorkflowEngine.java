package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeRunResult;
import com.example.miniagentflow.domain.NodeRunStatus;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowEdge;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.domain.WorkflowRunResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ParallelWorkflowEngine {

    private final WorkflowValidator workflowValidator;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final int parallelism;

    @Autowired
    public ParallelWorkflowEngine(WorkflowValidator workflowValidator,
                                  NodeExecutorRegistry nodeExecutorRegistry,
                                  @Value("${agentflow.engine.parallelism:4}") int parallelism) {
        this.workflowValidator = workflowValidator;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.parallelism = parallelism;
    }

    public WorkflowRunResult run(WorkflowDefinition workflow, Map<String, Object> inputs) {
        List<String> topologicalOrder = workflowValidator.validateAndSort(workflow, inputs);
        Map<String, WorkflowNode> nodeMap = buildNodeMap(workflow);
        Map<String, List<String>> adjacency = buildAdjacency(workflow, nodeMap.keySet());
        Map<String, AtomicInteger> remainingDeps = buildRemainingDeps(workflow, nodeMap.keySet());

        NodeExecutionContext context = new NodeExecutionContext(inputs);
        Map<String, NodeRunResult> nodeResultMap = new ConcurrentHashMap<>();
        BlockingQueue<String> readyQueue = new LinkedBlockingQueue<>();

        for (String nodeId : nodeMap.keySet()) {
            if (remainingDeps.get(nodeId).get() == 0) {
                readyQueue.offer(nodeId);
            }
        }

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger workerParallelism = new AtomicInteger(Math.max(1, Math.min(parallelism, nodeMap.size())));

        try (var executor = Executors.newFixedThreadPool(workerParallelism.get())) {
            List<CompletableFuture<Void>> workers = IntStream.range(0, workerParallelism.get())
                    .mapToObj(index -> CompletableFuture.runAsync(
                            () -> workerLoop(readyQueue, nodeMap, adjacency, remainingDeps, context, nodeResultMap, completedCount, failed),
                            executor
                    ))
                    .toList();
            CompletableFuture.allOf(workers.toArray(new CompletableFuture[0])).join();
        }

        List<NodeRunResult> orderedResults = new ArrayList<>();
        for (String nodeId : topologicalOrder) {
            NodeRunResult result = nodeResultMap.get(nodeId);
            if (result != null) {
                orderedResults.add(result);
            }
        }

        if (!failed.get() && completedCount.get() != nodeMap.size()) {
            failed.set(true);
        }

        return WorkflowRunResult.builder()
                .status(failed.get() ? "FAILED" : "SUCCESS")
                .nodeResults(orderedResults)
                .contextSnapshot(context.snapshotVariables())
                .build();
    }

    private void workerLoop(BlockingQueue<String> readyQueue,
                            Map<String, WorkflowNode> nodeMap,
                            Map<String, List<String>> adjacency,
                            Map<String, AtomicInteger> remainingDeps,
                            NodeExecutionContext context,
                            Map<String, NodeRunResult> nodeResultMap,
                            AtomicInteger completedCount,
                            AtomicBoolean failed) {
        while (true) {
            if (failed.get()) {
                return;
            }
            if (completedCount.get() >= nodeMap.size()) {
                return;
            }

            String nodeId;
            try {
                nodeId = readyQueue.poll(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return;
            }

            if (nodeId == null) {
                continue;
            }

            WorkflowNode node = nodeMap.get(nodeId);
            NodeRunResult result = nodeExecutorRegistry.getExecutor(node.getType()).execute(context, node);
            nodeResultMap.put(nodeId, result);
            context.putNodeOutput(nodeId, result.getOutput());

            if (result.getStatus() == NodeRunStatus.FAILED) {
                failed.compareAndSet(false, true);
                completedCount.incrementAndGet();
                continue;
            }

            if (result.getOutput() != null) {
                result.getOutput().forEach(context::putVariable);
            }
            completedCount.incrementAndGet();
            for (String next : adjacency.get(nodeId)) {
                if (remainingDeps.get(next).decrementAndGet() == 0 && !failed.get()) {
                    readyQueue.offer(next);
                }
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
