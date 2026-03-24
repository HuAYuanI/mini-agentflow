package com.example.miniagentflow.engine;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkflowThreadContext {

    // 【执行ID】：工作流的唯一执行ID
    private final String executionId;
    // 【引擎模式】：工作流的执行模式
    private final String engineMode;
    // 【开始时间】：工作流的开始时间
    private final long startedAtMillis;
}
