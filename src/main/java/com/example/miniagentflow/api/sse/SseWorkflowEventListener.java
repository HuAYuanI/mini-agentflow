package com.example.miniagentflow.api.sse;

import com.example.miniagentflow.domain.WorkflowExecutionEvent;
import com.example.miniagentflow.engine.event.WorkflowEventListener;
import java.io.IOException;

public class SseWorkflowEventListener implements WorkflowEventListener {

    private final WorkflowSseEventSender workflowSseEventSender;

    public SseWorkflowEventListener(WorkflowSseEventSender workflowSseEventSender) {
        this.workflowSseEventSender = workflowSseEventSender;
    }

    @Override
    public void onEvent(WorkflowExecutionEvent event) {
        try {
            workflowSseEventSender.sendEvent(event);
        } catch (IOException ioException) {
            workflowSseEventSender.completeWithError(ioException);
            throw new IllegalStateException("Failed to send SSE event", ioException);
        }
    }
}
