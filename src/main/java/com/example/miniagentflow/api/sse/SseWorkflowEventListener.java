package com.example.miniagentflow.api.sse;

import com.example.miniagentflow.domain.WorkflowExecutionEvent;
import com.example.miniagentflow.engine.event.WorkflowEventListener;
import java.io.IOException;

/**
 * SSE工作流事件监听器：将工作流事件转换为SSE事件并发送
 */
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
