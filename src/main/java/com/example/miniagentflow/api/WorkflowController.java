package com.example.miniagentflow.api;

import com.example.miniagentflow.api.dto.WorkflowExecuteRequest;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.service.WorkflowExecutionService;
import com.example.miniagentflow.service.WorkflowSseService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowSseService workflowSseService;

    public WorkflowController(WorkflowExecutionService workflowExecutionService,
            WorkflowSseService workflowSseService) {
        this.workflowExecutionService = workflowExecutionService;
        this.workflowSseService = workflowSseService;
    }

    @PostMapping("/execute")
    public WorkflowRunResult execute(@Valid @RequestBody WorkflowExecuteRequest request) {
        return workflowExecutionService.execute(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamExecute(@Valid @RequestBody WorkflowExecuteRequest request) {
        return workflowSseService.streamExecute(request);
    }
}
