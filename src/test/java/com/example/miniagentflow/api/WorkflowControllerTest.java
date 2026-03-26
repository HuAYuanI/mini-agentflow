package com.example.miniagentflow.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.miniagentflow.api.dto.WorkflowExecuteRequest;
import com.example.miniagentflow.domain.EngineMode;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.service.WorkflowOrchestratorService;
import com.example.miniagentflow.service.WorkflowSseService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class WorkflowControllerTest {

    @Test
    void shouldDelegateExecuteRequest() {
        WorkflowOrchestratorService workflowOrchestratorService = mock(WorkflowOrchestratorService.class);
        WorkflowSseService workflowSseService = mock(WorkflowSseService.class);
        WorkflowController workflowController = new WorkflowController(workflowOrchestratorService, workflowSseService);
        WorkflowExecuteRequest request = buildRequest();
        WorkflowRunResult expected = WorkflowRunResult.builder().status("SUCCESS").build();

        when(workflowOrchestratorService.execute(request.getWorkflow(), request.getInputs(), request.getEngineMode()))
                .thenReturn(expected);

        WorkflowRunResult actual = workflowController.execute(request);
        Assertions.assertSame(expected, actual);
    }

    @Test
    void shouldDelegateStreamRequest() {
        WorkflowOrchestratorService workflowOrchestratorService = mock(WorkflowOrchestratorService.class);
        WorkflowSseService workflowSseService = mock(WorkflowSseService.class);
        WorkflowController workflowController = new WorkflowController(workflowOrchestratorService, workflowSseService);
        WorkflowExecuteRequest request = buildRequest();
        SseEmitter emitter = new SseEmitter();

        when(workflowSseService.streamExecute(request)).thenReturn(emitter);

        SseEmitter actual = workflowController.streamExecute(request);
        Assertions.assertSame(emitter, actual);
    }

    private WorkflowExecuteRequest buildRequest() {
        return WorkflowExecuteRequest.builder()
                .workflow(WorkflowDefinition.builder().build())
                .engineMode(EngineMode.SERIAL)
                .build();
    }
}
