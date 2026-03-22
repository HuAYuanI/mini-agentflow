package com.example.miniagentflow.engine;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkflowThreadContext {

    private final String executionId;
    private final String engineMode;
    private final long startedAtMillis;
}
