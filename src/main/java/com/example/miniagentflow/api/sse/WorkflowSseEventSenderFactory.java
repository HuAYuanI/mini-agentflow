package com.example.miniagentflow.api.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface WorkflowSseEventSenderFactory {

    WorkflowSseEventSender create(SseEmitter emitter);
}
