package com.example.miniagentflow.engine;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.example.miniagentflow.domain.EngineMode;
import java.util.UUID;

// 【工作流线程上下文持有者】：工作流线程上下文的持有者
public final class WorkflowThreadContextHolder {

    // 【线程上下文】：线程本地存储
    private static final TransmittableThreadLocal<WorkflowThreadContext> HOLDER = new TransmittableThreadLocal<>();

    // 私有构造函数
    private WorkflowThreadContextHolder() {
    }

    // 【初始化运行】：初始化线程上下文
    public static WorkflowThreadContext init(EngineMode engineMode) {
        WorkflowThreadContext context = WorkflowThreadContext.builder()
                .executionId(UUID.randomUUID().toString()) // 生成执行ID
                .engineMode(engineMode.name()) // 设置引擎模式
                .startedAtMillis(System.currentTimeMillis()) // 设置开始时间
                .build();
        HOLDER.set(context); // 设置线程上下文
        return context; // 返回线程上下文
    }

    // 【初始化并行运行】：初始化并行运行的线程上下文
    public static WorkflowThreadContext initParallelRun() {
        return init(EngineMode.PARALLEL);
    }

    // 【初始化串行运行】：初始化串行运行的线程上下文
    public static WorkflowThreadContext initSerialRun() {
        return init(EngineMode.SERIAL);
    }

    // 【获取线程上下文】：获取当前线程的上下文
    public static WorkflowThreadContext get() {
        return HOLDER.get();
    }

    // 【清除线程上下文】：清除当前线程的上下文
    public static void clear() {
        HOLDER.remove();
    }
}
