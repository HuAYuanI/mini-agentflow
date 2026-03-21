package com.example.miniagentflow.api;

import com.example.miniagentflow.api.dto.WorkflowExecuteRequest;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.service.WorkflowOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final WorkflowOrchestratorService workflowOrchestratorService;

    public WorkflowController(WorkflowOrchestratorService workflowOrchestratorService) {
        this.workflowOrchestratorService = workflowOrchestratorService;
    }

    @PostMapping("/execute")
    public WorkflowRunResult execute(@Valid @RequestBody WorkflowExecuteRequest request) {
        return workflowOrchestratorService.execute(request.getWorkflow(), request.getInputs(), request.getEngineMode());
    }
}
