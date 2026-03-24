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

    // 【节点超时执行器】：用于执行节点并设置超时时间，使用虚拟线程
    private static final ExecutorService NODE_TIMEOUT_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    // 【执行节点】：执行节点并返回执行结果
    @Override
    public final NodeRunResult execute(NodeExecutionContext context, WorkflowNode node) {
        int retryTimes = parseIntConfig(node.getConfig().get("retryTimes"), 0); // 获取重试次数
        long timeoutMs = parseLongConfig(node.getConfig().get("timeoutMs"), 0L); // 获取超时时间
        int maxAttempts = Math.max(1, retryTimes + 1); // 计算最大尝试次数
        Exception lastException = null; // 记录最后一次异常

        for (int attempt = 1; attempt <= maxAttempts; attempt++) { // 循环执行节点
            try {
                beforeExecute(context, node); // 执行前置操作
                Map<String, Object> output = executeWithTimeout(context, node, timeoutMs); // 执行节点逻辑
                NodeRunResult result = NodeRunResult.success(node.getId(), output); // 创建成功结果
                afterExecute(context, node, result); // 执行后置操作
                return result; // 返回结果
            } catch (Exception ex) {
                lastException = ex; // 记录异常，继续下一次循环
            }
        }

        // 创建失败结果
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

    // 【带超时的节点执行】：执行节点并设置超时时间
    private Map<String, Object> executeWithTimeout(NodeExecutionContext context, WorkflowNode node, long timeoutMs)
            throws Exception {
        // 如果超时时间小于等于0，直接执行节点
        if (timeoutMs <= 0) {
            return doExecute(context, node);
        }
        // 使用虚拟线程执行节点
        Future<Map<String, Object>> future = NODE_TIMEOUT_EXECUTOR.submit(() -> doExecute(context, node));
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS); // 获取执行结果
        } catch (TimeoutException timeoutException) { // 超时异常
            future.cancel(true); // 取消执行
            throw new RuntimeException("Node " + node.getId() + " timeout after " + timeoutMs + "ms"); // 抛出超时异常
        } catch (InterruptedException interruptedException) { // 中断异常
            future.cancel(true); // 取消执行
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException("Node " + node.getId() + " interrupted");
        } catch (ExecutionException executionException) { // 执行异常
            Throwable cause = executionException.getCause();
            if (cause instanceof Exception exception) { // 如果是Exception类型，直接抛出
                throw exception;
            }
            throw new RuntimeException(cause); // 否则包装成RuntimeException抛出
        }
    }

    // 【解析配置】：解析配置值
    private int parseIntConfig(Object configValue, int defaultValue) {
        // 如果配置值为空，返回默认值
        if (configValue == null) {
            return defaultValue;
        }
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(configValue))); // 转换为int类型
        } catch (NumberFormatException numberFormatException) {
            return defaultValue;
        }
    }

    // 【解析配置】：解析配置值
    private long parseLongConfig(Object configValue, long defaultValue) {
        // 如果配置值为空，返回默认值
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
