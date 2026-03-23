package com.example.miniagentflow.engine;

import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.example.miniagentflow.domain.NodeRunResult;
import com.example.miniagentflow.domain.NodeRunStatus;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowEdge;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.error.NodeErrorStrategySelector;
import com.example.miniagentflow.engine.error.NodeFailureDecision;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
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
    private final NodeErrorStrategySelector nodeErrorStrategySelector;

    @Autowired
    public ParallelWorkflowEngine(WorkflowValidator workflowValidator,
                                  NodeExecutorRegistry nodeExecutorRegistry,
                                  @Value("${agentflow.engine.parallelism:4}") int parallelism) {
        this(workflowValidator, nodeExecutorRegistry, parallelism, new NodeErrorStrategySelector());
    }

    public ParallelWorkflowEngine(WorkflowValidator workflowValidator,
                                  NodeExecutorRegistry nodeExecutorRegistry,
                                  int parallelism,
                                  NodeErrorStrategySelector nodeErrorStrategySelector) {
        this.workflowValidator = workflowValidator;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.parallelism = parallelism;
        this.nodeErrorStrategySelector = nodeErrorStrategySelector;
    }

    public WorkflowRunResult run(WorkflowDefinition workflow, Map<String, Object> inputs) {
        List<String> topologicalOrder = workflowValidator.validateAndSort(workflow, inputs);
        Map<String, WorkflowNode> nodeMap = buildNodeMap(workflow);
        Map<String, List<String>> adjacency = buildAdjacency(workflow, nodeMap.keySet());
        Map<String, AtomicInteger> remainingDeps = buildRemainingDeps(workflow, nodeMap.keySet());

        NodeExecutionContext context = new NodeExecutionContext(inputs);
        WorkflowThreadContext threadContext = WorkflowThreadContextHolder.initParallelRun();
        context.putVariable("executionId", threadContext.getExecutionId());
        context.putVariable("engineMode", threadContext.getEngineMode());
        context.putVariable("startedAtMillis", threadContext.getStartedAtMillis());

        Map<String, NodeRunResult> nodeResultMap = new ConcurrentHashMap<>();
        BlockingQueue<String> readyQueue = new LinkedBlockingQueue<>();
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicBoolean handledFailure = new AtomicBoolean(false);
        AtomicInteger inFlight = new AtomicInteger(0);
        int workerParallelism = Math.max(1, Math.min(parallelism, nodeMap.size()));

        for (String nodeId : nodeMap.keySet()) {
            if (remainingDeps.get(nodeId).get() == 0) {
                readyQueue.offer(nodeId);
            }
        }

        try {
            try (ExecutorService executor = Executors.newFixedThreadPool(workerParallelism)) {
                ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);
                List<CompletableFuture<Void>> workers = IntStream.range(0, workerParallelism)
                        .mapToObj(index -> CompletableFuture.runAsync(
                                TtlRunnable.get(() -> workerLoop(
                                        readyQueue,
                                        nodeMap,
                                        adjacency,
                                        remainingDeps,
                                        context,
                                        nodeResultMap,
                                        interrupted,
                                        handledFailure,
                                        inFlight)),
                                ttlExecutor))
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

            String status = interrupted.get() ? "FAILED" : handledFailure.get() ? "PARTIAL_SUCCESS" : "SUCCESS";
            return WorkflowRunResult.builder()
                    .status(status)
                    .nodeResults(orderedResults)
                    .contextSnapshot(context.snapshotVariables())
                    .build();
        } finally {
            WorkflowThreadContextHolder.clear();
        }
    }

    private void workerLoop(BlockingQueue<String> readyQueue,
                            Map<String, WorkflowNode> nodeMap,
                            Map<String, List<String>> adjacency,
                            Map<String, AtomicInteger> remainingDeps,
                            NodeExecutionContext context,
                            Map<String, NodeRunResult> nodeResultMap,
                            AtomicBoolean interrupted,
                            AtomicBoolean handledFailure,
                            AtomicInteger inFlight) {
        while (true) {
            if (interrupted.get()) {
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
                if (readyQueue.isEmpty() && inFlight.get() == 0) {
                    return;
                }
                continue;
            }

            inFlight.incrementAndGet();
            try {
                if (interrupted.get()) {
                    return;
                }

                WorkflowNode node = nodeMap.get(nodeId);
                NodeRunResult result = nodeExecutorRegistry.getExecutor(node.getType()).execute(context, node);
                nodeResultMap.put(nodeId, result);
                context.putNodeOutput(nodeId, result.getOutput());

                if (result.getStatus() == NodeRunStatus.FAILED) {
                    handledFailure.set(true);
                    context.putVariable("lastErrorNode", nodeId);
                    context.putVariable("lastErrorMessage", result.getErrorMessage());
                    NodeFailureDecision decision = nodeErrorStrategySelector.decide(node, adjacency.get(nodeId));
                    if (decision.isInterruptWorkflow()) {
                        interrupted.compareAndSet(false, true);
                    } else {
                        releaseNextNodes(decision.getNextNodeIds(), remainingDeps, readyQueue, interrupted);
                    }
                    continue;
                }

                if (result.getOutput() != null) {
                    result.getOutput().forEach(context::putVariable);
                }
                releaseNextNodes(adjacency.get(nodeId), remainingDeps, readyQueue, interrupted);
            } finally {
                inFlight.decrementAndGet();
            }
        }
    }

    private void releaseNextNodes(List<String> nextNodes,
                                  Map<String, AtomicInteger> remainingDeps,
                                  BlockingQueue<String> readyQueue,
                                  AtomicBoolean interrupted) {
        if (nextNodes == null || interrupted.get()) {
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
