package com.example.miniagentflow.engine;

import com.alibaba.ttl.TransmittableThreadLocal;
import java.util.UUID;

public final class WorkflowThreadContextHolder {

    private static final TransmittableThreadLocal<WorkflowThreadContext> HOLDER = new TransmittableThreadLocal<>();

    private WorkflowThreadContextHolder() {
    }

    public static WorkflowThreadContext initParallelRun() {
        WorkflowThreadContext context = WorkflowThreadContext.builder()
                .executionId(UUID.randomUUID().toString())
                .engineMode("PARALLEL")
                .startedAtMillis(System.currentTimeMillis())
                .build();
        HOLDER.set(context);
        return context;
    }

    public static WorkflowThreadContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
