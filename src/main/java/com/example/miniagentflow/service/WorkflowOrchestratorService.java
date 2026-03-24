package com.example.miniagentflow.service;

import com.example.miniagentflow.domain.EngineMode;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.ParallelWorkflowEngine;
import com.example.miniagentflow.engine.SerialWorkflowEngine;
import com.example.miniagentflow.engine.event.NoopWorkflowEventListener;
import com.example.miniagentflow.engine.event.WorkflowEventListener;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkflowOrchestratorService {

    private final SerialWorkflowEngine serialWorkflowEngine;
    private final ParallelWorkflowEngine parallelWorkflowEngine;

    public WorkflowOrchestratorService(SerialWorkflowEngine serialWorkflowEngine,
                                       ParallelWorkflowEngine parallelWorkflowEngine) {
        this.serialWorkflowEngine = serialWorkflowEngine;
        this.parallelWorkflowEngine = parallelWorkflowEngine;
    }

    public WorkflowRunResult execute(WorkflowDefinition workflow, Map<String, Object> inputs, EngineMode engineMode) {
        return execute(workflow, inputs, engineMode, NoopWorkflowEventListener.INSTANCE);
    }

    public WorkflowRunResult execute(WorkflowDefinition workflow,
            Map<String, Object> inputs,
            EngineMode engineMode,
            WorkflowEventListener eventListener) {
        if (engineMode == EngineMode.PARALLEL) {
            return parallelWorkflowEngine.run(workflow, inputs, eventListener);
        }
        return serialWorkflowEngine.run(workflow, inputs, eventListener);
    }
}
