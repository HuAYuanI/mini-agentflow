package com.example.miniagentflow.engine;

import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.threadpool.TtlExecutors;
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

// 并行工作流引擎
@Component
public class ParallelWorkflowEngine {

    private final WorkflowValidator workflowValidator; // 工作流验证器
    private final NodeExecutorRegistry nodeExecutorRegistry; // 节点执行器注册表
    private final int parallelism; // 并行度
    private final NodeErrorStrategySelector nodeErrorStrategySelector; // 节点错误策略选择器

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

    // 【执行工作流】：执行工作流
    public WorkflowRunResult run(WorkflowDefinition workflow, Map<String, Object> inputs) {
        return run(workflow, inputs, NoopWorkflowEventListener.INSTANCE);
    }

    public WorkflowRunResult run(WorkflowDefinition workflow,
            Map<String, Object> inputs,
            WorkflowEventListener eventListener) {
        // 验证工作流并获取拓扑排序
        List<String> topologicalOrder = workflowValidator.validateAndSort(workflow, inputs);
        // 构建节点映射
        Map<String, WorkflowNode> nodeMap = buildNodeMap(workflow);
        // 构建邻接表
        Map<String, List<String>> adjacency = buildAdjacency(workflow, nodeMap.keySet());
        // 构建剩余依赖数
        Map<String, AtomicInteger> remainingDeps = buildRemainingDeps(workflow, nodeMap.keySet());

        CollectingWorkflowEventListener collector = new CollectingWorkflowEventListener();
        WorkflowEventListener compositeListener = CompositeWorkflowEventListener.of(collector, eventListener);
        // 初始化节点执行上下文
        WorkflowThreadContext threadContext = WorkflowThreadContextHolder.init(EngineMode.PARALLEL);
        NodeExecutionContext context = new NodeExecutionContext(inputs, threadContext, compositeListener);
        // 初始化线程上下文
        // 设置执行ID
        context.putVariable("executionId", threadContext.getExecutionId());
        // 设置引擎模式
        context.putVariable("engineMode", threadContext.getEngineMode());
        // 设置开始时间
        context.putVariable("startedAtMillis", threadContext.getStartedAtMillis());
        context.publishWorkflowEvent(
                WorkflowEventType.WORKFLOW_STARTED,
                "RUNNING",
                "Workflow execution started",
                Map.of("nodeCount", nodeMap.size()));

        // 节点执行结果映射
        Map<String, NodeRunResult> nodeResultMap = new ConcurrentHashMap<>();
        // 就绪队列
        BlockingQueue<String> readyQueue = new LinkedBlockingQueue<>();
        // 中断标志
        AtomicBoolean interrupted = new AtomicBoolean(false);
        // 错误处理标志
        AtomicBoolean handledFailure = new AtomicBoolean(false);
        // 并发执行数
        AtomicInteger inFlight = new AtomicInteger(0);
        // 工作线程并行度
        int workerParallelism = Math.max(1, Math.min(parallelism, nodeMap.size()));

        // 初始化就绪队列：将入度为0的节点加入就绪队列
        for (String nodeId : nodeMap.keySet()) {
            if (remainingDeps.get(nodeId).get() == 0) {
                readyQueue.offer(nodeId);
            }
        }

        // 使用try-finally确保线程上下文被清理
        try {
            // 创建固定大小的线程池
            try (ExecutorService executor = Executors.newFixedThreadPool(workerParallelism)) {
                // 使用TtlExecutors包装线程池，支持TTL
                ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);
                // 创建工作线程
                List<CompletableFuture<Void>> workers = IntStream.range(0, workerParallelism)
                        .mapToObj(index -> CompletableFuture.runAsync(
                                TtlRunnable.get(() -> workerLoop(
                                        readyQueue, // 就绪队列
                                        nodeMap, // 节点映射
                                        adjacency, // 邻接表
                                        remainingDeps, // 剩余依赖数
                                        context, // 节点执行上下文
                                        nodeResultMap, // 节点执行结果映射
                                        interrupted, // 中断标志
                                        handledFailure, // 错误处理标志
                                        inFlight)), // 并发执行数
                                ttlExecutor))
                        .toList();
                CompletableFuture.allOf(workers.toArray(new CompletableFuture[0])).join();
            }

            // 按照拓扑排序整理节点执行结果
            List<NodeRunResult> orderedResults = new ArrayList<>();
            // 遍历拓扑排序的节点ID
            for (String nodeId : topologicalOrder) {
                // 获取节点执行结果
                NodeRunResult result = nodeResultMap.get(nodeId);
                // 如果节点执行结果不为null，则添加到有序结果列表中
                if (result != null) {
                    orderedResults.add(result);
                }
            }

            // 根据中断标志和错误处理标志确定工作流状态
            String status = interrupted.get() ? "FAILED" : handledFailure.get() ? "PARTIAL_SUCCESS" : "SUCCESS";
            context.publishWorkflowEvent(
                    WorkflowEventType.WORKFLOW_COMPLETED,
                    status,
                    "Workflow execution completed",
                    Map.of("executedNodeCount", orderedResults.size()));
            // 构建工作流运行结果
            return WorkflowRunResult.builder()
                    .status(status)
                    .nodeResults(orderedResults)
                    .events(collector.snapshot())
                    .contextSnapshot(context.snapshotVariables())
                    .build();
        } finally {
            WorkflowThreadContextHolder.clear(); // 清除线程上下文
        }
    }

    // 工作线程循环
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
            // 如果中断标志为true，则直接返回
            if (interrupted.get()) {
                return;
            }

            String nodeId;
            // 从就绪队列中获取节点ID
            try {
                nodeId = readyQueue.poll(100, TimeUnit.MILLISECONDS); // 设置超时时间（100ms），避免阻塞
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt(); // 恢复中断状态
                return;
            }

            // 如果节点ID为null，则直接返回
            if (nodeId == null) {
                // 如果就绪队列为空且并发执行数为0，则直接返回
                if (readyQueue.isEmpty() && inFlight.get() == 0) {
                    return;
                }
                continue;
            }

            // 并发执行数加1
            inFlight.incrementAndGet();
            try {
                // 如果中断标志为true，则直接返回
                if (interrupted.get()) {
                    return;
                }

                // 获取节点
                WorkflowNode node = nodeMap.get(nodeId);
                // 执行节点
                NodeRunResult result = nodeExecutorRegistry.getExecutor(node.getType()).execute(context, node);
                // 存储节点执行结果
                nodeResultMap.put(nodeId, result);
                // 存储节点输出
                context.putNodeOutput(nodeId, result.getOutput());

                // 如果节点执行失败
                if (result.getStatus() == NodeRunStatus.FAILED) {
                    // 设置错误处理标志
                    handledFailure.set(true);
                    // 设置错误节点
                    context.putVariable("lastErrorNode", nodeId);
                    // 设置错误信息
                    context.putVariable("lastErrorMessage", result.getErrorMessage());
                    // 根据错误处理策略决定后续操作
                    NodeFailureDecision decision = nodeErrorStrategySelector.decide(node, adjacency.get(nodeId));
                    // 如果决策是中断工作流
                    if (decision.isInterruptWorkflow()) {
                        interrupted.compareAndSet(false, true); // 设置中断标志
                    } else {
                        // 释放后续节点
                        releaseNextNodes(decision.getNextNodeIds(), remainingDeps, readyQueue, interrupted);
                    }
                    continue;
                }

                // 如果节点输出不为null
                if (result.getOutput() != null) {
                    // 将节点输出放入变量池
                    result.getOutput().forEach(context::putVariable);
                }
                // 释放后续节点
                releaseNextNodes(adjacency.get(nodeId), remainingDeps, readyQueue, interrupted);
            } finally {
                inFlight.decrementAndGet(); // 并发执行数减1
            }
        }
    }

    // 释放后续节点
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

    // 构建节点映射
    private Map<String, WorkflowNode> buildNodeMap(WorkflowDefinition workflow) {
        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNode node : workflow.getNodes()) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    // 构建邻接表
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

    // 构建剩余依赖数
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
