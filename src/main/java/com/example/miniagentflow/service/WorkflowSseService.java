package com.example.miniagentflow.service;

import com.example.miniagentflow.api.dto.WorkflowExecuteRequest;
import com.example.miniagentflow.api.sse.SseWorkflowEventListener;
import com.example.miniagentflow.api.sse.WorkflowSseEventSender;
import com.example.miniagentflow.api.sse.WorkflowSseEventSenderFactory;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.event.WorkflowEventListener;
import java.io.IOException;
import java.util.concurrent.Executor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class WorkflowSseService {

    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowSseEventSenderFactory workflowSseEventSenderFactory;
    private final Executor workflowStreamExecutor;

    public WorkflowSseService(WorkflowExecutionService workflowExecutionService,
            WorkflowSseEventSenderFactory workflowSseEventSenderFactory) {
        this(workflowExecutionService, workflowSseEventSenderFactory, createDefaultExecutor());
    }

    WorkflowSseService(WorkflowExecutionService workflowExecutionService,
            WorkflowSseEventSenderFactory workflowSseEventSenderFactory,
            Executor workflowStreamExecutor) {
        this.workflowExecutionService = workflowExecutionService;
        this.workflowSseEventSenderFactory = workflowSseEventSenderFactory;
        this.workflowStreamExecutor = workflowStreamExecutor;
    }

    public SseEmitter streamExecute(WorkflowExecuteRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        WorkflowSseEventSender sender = workflowSseEventSenderFactory.create(emitter);
        WorkflowEventListener eventListener = new SseWorkflowEventListener(sender);
        workflowStreamExecutor.execute(() -> executeWorkflow(request, sender, eventListener));
        return emitter;
    }

    void executeWorkflow(WorkflowExecuteRequest request,
            WorkflowSseEventSender sender,
            WorkflowEventListener eventListener) {
        try {
            WorkflowRunResult result = workflowExecutionService.execute(request, eventListener);
            sender.sendResult(result);
            sender.complete();
        } catch (Exception exception) {
            handleStreamFailure(sender, exception);
        }
    }

    private void handleStreamFailure(WorkflowSseEventSender sender, Exception exception) {
        try {
            sender.sendError(exception.getMessage());
        } catch (IOException ioException) {
            sender.completeWithError(ioException);
            return;
        }
        sender.completeWithError(exception);
    }

    private static Executor createDefaultExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("workflow-sse-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
