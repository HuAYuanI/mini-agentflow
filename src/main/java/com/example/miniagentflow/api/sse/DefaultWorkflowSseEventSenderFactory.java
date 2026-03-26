package com.example.miniagentflow.api.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class DefaultWorkflowSseEventSenderFactory implements WorkflowSseEventSenderFactory {

    @Override
    public WorkflowSseEventSender create(SseEmitter emitter) {
        return new SseEmitterWorkflowEventSender(emitter);
    }
}
