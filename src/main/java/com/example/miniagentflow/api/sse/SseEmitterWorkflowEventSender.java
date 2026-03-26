package com.example.miniagentflow.api.sse;

import com.example.miniagentflow.domain.WorkflowExecutionEvent;
import com.example.miniagentflow.domain.WorkflowRunResult;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SseEmitterWorkflowEventSender implements WorkflowSseEventSender {

    private final SseEmitter emitter;

    public SseEmitterWorkflowEventSender(SseEmitter emitter) {
        this.emitter = emitter;
        this.emitter.onTimeout(this.emitter::complete);
    }

    @Override
    public void sendEvent(WorkflowExecutionEvent event) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event.getType().name())
                .data(event));
    }

    @Override
    public void sendResult(WorkflowRunResult result) throws IOException {
        emitter.send(SseEmitter.event()
                .name("WORKFLOW_RESULT")
                .data(result));
    }

    @Override
    public void sendError(String message) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message == null ? "Workflow stream failed" : message);
        emitter.send(SseEmitter.event()
                .name("WORKFLOW_ERROR")
                .data(payload));
    }

    @Override
    public void complete() {
        emitter.complete();
    }

    @Override
    public void completeWithError(Throwable throwable) {
        emitter.completeWithError(throwable);
    }
}
