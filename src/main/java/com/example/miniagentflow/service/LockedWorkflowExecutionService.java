package com.example.miniagentflow.service;

import com.example.miniagentflow.api.dto.WorkflowExecuteRequest;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.event.WorkflowEventListener;
import com.example.miniagentflow.lock.DistributedLock;
import org.springframework.stereotype.Service;

@Service
public class LockedWorkflowExecutionService {

    private final WorkflowOrchestratorService workflowOrchestratorService;

    public LockedWorkflowExecutionService(WorkflowOrchestratorService workflowOrchestratorService) {
        this.workflowOrchestratorService = workflowOrchestratorService;
    }

    @DistributedLock(
            key = "'workflow:' + #request.lockKey",
            type = "#request.lockType != null ? #request.lockType.name() : 'REENTRANT'",
            waitTimeMs = "#request.lockWaitTimeMs != null ? #request.lockWaitTimeMs : 0L",
            leaseTimeMs = "#request.lockLeaseTimeMs != null ? #request.lockLeaseTimeMs : 30000L"
    )
    public WorkflowRunResult execute(WorkflowExecuteRequest request, WorkflowEventListener eventListener) {
        return workflowOrchestratorService.execute(
                request.getWorkflow(),
                request.getInputs(),
                request.getEngineMode(),
                eventListener);
    }
}
