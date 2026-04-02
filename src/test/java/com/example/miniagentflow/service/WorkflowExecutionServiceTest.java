package com.example.miniagentflow.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.miniagentflow.api.dto.WorkflowExecuteRequest;
import com.example.miniagentflow.domain.EngineMode;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.event.NoopWorkflowEventListener;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WorkflowExecutionServiceTest {

    @Test
    void shouldDelegateDirectlyWhenLockKeyIsBlank() {
        WorkflowOrchestratorService workflowOrchestratorService = mock(WorkflowOrchestratorService.class);
        LockedWorkflowExecutionService lockedWorkflowExecutionService = mock(LockedWorkflowExecutionService.class);
        WorkflowExecutionService workflowExecutionService =
                new WorkflowExecutionService(workflowOrchestratorService, lockedWorkflowExecutionService);
        WorkflowExecuteRequest request = WorkflowExecuteRequest.builder()
                .workflow(WorkflowDefinition.builder().build())
                .inputs(Map.of("input", "hello"))
                .engineMode(EngineMode.SERIAL)
                .build();
        WorkflowRunResult expected = WorkflowRunResult.builder().status("SUCCESS").build();

        when(workflowOrchestratorService.execute(request.getWorkflow(), request.getInputs(), request.getEngineMode(),
                NoopWorkflowEventListener.INSTANCE)).thenReturn(expected);

        WorkflowRunResult actual = workflowExecutionService.execute(request);

        Assertions.assertSame(expected, actual);
        verifyNoInteractions(lockedWorkflowExecutionService);
    }

    @Test
    void shouldDelegateToLockedServiceWhenLockKeyExists() {
        WorkflowOrchestratorService workflowOrchestratorService = mock(WorkflowOrchestratorService.class);
        LockedWorkflowExecutionService lockedWorkflowExecutionService = mock(LockedWorkflowExecutionService.class);
        WorkflowExecutionService workflowExecutionService =
                new WorkflowExecutionService(workflowOrchestratorService, lockedWorkflowExecutionService);
        WorkflowExecuteRequest request = WorkflowExecuteRequest.builder()
                .workflow(WorkflowDefinition.builder().build())
                .engineMode(EngineMode.SERIAL)
                .lockKey("resume-demo")
                .build();
        WorkflowRunResult expected = WorkflowRunResult.builder().status("SUCCESS").build();

        when(lockedWorkflowExecutionService.execute(request, NoopWorkflowEventListener.INSTANCE)).thenReturn(expected);

        WorkflowRunResult actual = workflowExecutionService.execute(request);

        Assertions.assertSame(expected, actual);
        verify(lockedWorkflowExecutionService).execute(request, NoopWorkflowEventListener.INSTANCE);
        verifyNoInteractions(workflowOrchestratorService);
    }
}
