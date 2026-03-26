package com.example.miniagentflow.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.miniagentflow.api.dto.WorkflowExecuteRequest;
import com.example.miniagentflow.api.sse.WorkflowSseEventSender;
import com.example.miniagentflow.api.sse.WorkflowSseEventSenderFactory;
import com.example.miniagentflow.domain.EngineMode;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowExecutionEvent;
import com.example.miniagentflow.domain.WorkflowEventType;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.event.WorkflowEventListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class WorkflowSseServiceTest {

    @Test
    void shouldStreamLifecycleEventsAndFinalResult() {
        WorkflowOrchestratorService workflowOrchestratorService = mock(WorkflowOrchestratorService.class);
        RecordingWorkflowSseEventSender sender = new RecordingWorkflowSseEventSender();
        WorkflowSseEventSenderFactory factory = emitter -> sender;
        Executor directExecutor = Runnable::run;
        WorkflowSseService workflowSseService =
                new WorkflowSseService(workflowOrchestratorService, factory, directExecutor);

        WorkflowRunResult workflowRunResult = WorkflowRunResult.builder()
                .status("SUCCESS")
                .build();

        doAnswer(invocation -> {
            WorkflowEventListener eventListener = invocation.getArgument(3);
            eventListener.onEvent(WorkflowExecutionEvent.builder()
                    .sequence(1L)
                    .timestamp(System.currentTimeMillis())
                    .type(WorkflowEventType.WORKFLOW_STARTED)
                    .executionId("exec-1")
                    .engineMode("SERIAL")
                    .message("Workflow execution started")
                    .build());
            return workflowRunResult;
        }).when(workflowOrchestratorService).execute(any(), anyMap(), any(), any());

        SseEmitter emitter = workflowSseService.streamExecute(buildRequest());

        Assertions.assertNotNull(emitter);
        Assertions.assertEquals(1, sender.events.size());
        Assertions.assertEquals(WorkflowEventType.WORKFLOW_STARTED, sender.events.getFirst().getType());
        Assertions.assertSame(workflowRunResult, sender.result);
        Assertions.assertTrue(sender.completed);
        Assertions.assertNull(sender.completedWithError);
    }

    @Test
    void shouldSendErrorEventWhenWorkflowExecutionFails() {
        WorkflowOrchestratorService workflowOrchestratorService = mock(WorkflowOrchestratorService.class);
        RecordingWorkflowSseEventSender sender = new RecordingWorkflowSseEventSender();
        WorkflowSseEventSenderFactory factory = emitter -> sender;
        Executor directExecutor = Runnable::run;
        WorkflowSseService workflowSseService =
                new WorkflowSseService(workflowOrchestratorService, factory, directExecutor);

        when(workflowOrchestratorService.execute(any(), anyMap(), any(), any()))
                .thenThrow(new RuntimeException("stream failed"));

        workflowSseService.streamExecute(buildRequest());

        Assertions.assertEquals("stream failed", sender.errorMessage);
        Assertions.assertNotNull(sender.completedWithError);
        Assertions.assertFalse(sender.completed);
    }

    private WorkflowExecuteRequest buildRequest() {
        return WorkflowExecuteRequest.builder()
                .workflow(WorkflowDefinition.builder().build())
                .engineMode(EngineMode.SERIAL)
                .build();
    }

    private static class RecordingWorkflowSseEventSender implements WorkflowSseEventSender {

        private final List<WorkflowExecutionEvent> events = new ArrayList<>();
        private WorkflowRunResult result;
        private String errorMessage;
        private boolean completed;
        private Throwable completedWithError;

        @Override
        public void sendEvent(WorkflowExecutionEvent event) {
            events.add(event);
        }

        @Override
        public void sendResult(WorkflowRunResult result) {
            this.result = result;
        }

        @Override
        public void sendError(String message) throws IOException {
            this.errorMessage = message;
        }

        @Override
        public void complete() {
            this.completed = true;
        }

        @Override
        public void completeWithError(Throwable throwable) {
            this.completedWithError = throwable;
        }
    }
}
