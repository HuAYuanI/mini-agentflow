package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeRunResult;
import com.example.miniagentflow.domain.NodeRunStatus;
import com.example.miniagentflow.domain.WorkflowNode;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// 【抽象节点执行器】：定义节点执行的模板方法，提供模板方法模式
public abstract class AbstractNodeExecutor implements NodeExecutor {

    private static final ExecutorService NODE_TIMEOUT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public final NodeRunResult execute(NodeExecutionContext context, WorkflowNode node) {
        int retryTimes = parseIntConfig(node.getConfig().get("retryTimes"), 0);
        long timeoutMs = parseLongConfig(node.getConfig().get("timeoutMs"), 0L);
        int maxAttempts = Math.max(1, retryTimes + 1);
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                beforeExecute(context, node);
                Map<String, Object> output = executeWithTimeout(context, node, timeoutMs);
                NodeRunResult result = NodeRunResult.success(node.getId(), output);
                afterExecute(context, node, result);
                return result;
            } catch (Exception ex) {
                lastException = ex;
            }
        }

        String errorMessage = lastException == null ? "Node execution failed" : lastException.getMessage();
        return NodeRunResult.builder()
                .nodeId(node.getId())
                .status(NodeRunStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }

    // 【执行前置操作】：节点执行前的准备工作，可由子类覆盖
    protected void beforeExecute(NodeExecutionContext context, WorkflowNode node) {
    }

    // 【执行节点逻辑】：节点的核心业务逻辑，必须由子类实现
    protected abstract Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node);

    // 【执行后置操作】：节点执行后的清理工作，可由子类覆盖
    protected void afterExecute(NodeExecutionContext context, WorkflowNode node, NodeRunResult result) {
    }

    private Map<String, Object> executeWithTimeout(NodeExecutionContext context, WorkflowNode node, long timeoutMs)
            throws Exception {
        if (timeoutMs <= 0) {
            return doExecute(context, node);
        }
        Future<Map<String, Object>> future = NODE_TIMEOUT_EXECUTOR.submit(() -> doExecute(context, node));
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            throw new RuntimeException("Node " + node.getId() + " timeout after " + timeoutMs + "ms");
        } catch (InterruptedException interruptedException) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Node " + node.getId() + " interrupted");
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(cause);
        }
    }

    private int parseIntConfig(Object configValue, int defaultValue) {
        if (configValue == null) {
            return defaultValue;
        }
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(configValue)));
        } catch (NumberFormatException numberFormatException) {
            return defaultValue;
        }
    }

    private long parseLongConfig(Object configValue, long defaultValue) {
        if (configValue == null) {
            return defaultValue;
        }
        try {
            return Math.max(0L, Long.parseLong(String.valueOf(configValue)));
        } catch (NumberFormatException numberFormatException) {
            return defaultValue;
        }
    }
}
