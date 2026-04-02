package com.example.miniagentflow.service;

import com.example.miniagentflow.api.dto.WorkflowExecuteRequest;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.event.NoopWorkflowEventListener;
import com.example.miniagentflow.engine.event.WorkflowEventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WorkflowExecutionService {

    private final WorkflowOrchestratorService workflowOrchestratorService;
    private final LockedWorkflowExecutionService lockedWorkflowExecutionService;

    public WorkflowExecutionService(WorkflowOrchestratorService workflowOrchestratorService,
            LockedWorkflowExecutionService lockedWorkflowExecutionService) {
        this.workflowOrchestratorService = workflowOrchestratorService;
        this.lockedWorkflowExecutionService = lockedWorkflowExecutionService;
    }

    public WorkflowRunResult execute(WorkflowExecuteRequest request) {
        return execute(request, NoopWorkflowEventListener.INSTANCE);
    }

    public WorkflowRunResult execute(WorkflowExecuteRequest request, WorkflowEventListener eventListener) {
        if (StringUtils.hasText(request.getLockKey())) {
            return lockedWorkflowExecutionService.execute(request, eventListener);
        }
        return workflowOrchestratorService.execute(
                request.getWorkflow(),
                request.getInputs(),
                request.getEngineMode(),
                eventListener);
    }
}
