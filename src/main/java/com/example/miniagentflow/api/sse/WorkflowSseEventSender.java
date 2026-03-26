package com.example.miniagentflow.api.sse;

import com.example.miniagentflow.domain.WorkflowExecutionEvent;
import com.example.miniagentflow.domain.WorkflowRunResult;
import java.io.IOException;

public interface WorkflowSseEventSender {

    void sendEvent(WorkflowExecutionEvent event) throws IOException;

    void sendResult(WorkflowRunResult result) throws IOException;

    void sendError(String message) throws IOException;

    void complete();

    void completeWithError(Throwable throwable);
}
