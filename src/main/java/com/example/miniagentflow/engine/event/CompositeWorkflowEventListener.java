package com.example.miniagentflow.engine.event;

import com.example.miniagentflow.domain.WorkflowExecutionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeWorkflowEventListener implements WorkflowEventListener {

    private final List<WorkflowEventListener> listeners;

    private CompositeWorkflowEventListener(List<WorkflowEventListener> listeners) {
        this.listeners = listeners;
    }

    public static WorkflowEventListener of(WorkflowEventListener... listeners) {
        List<WorkflowEventListener> validListeners = new ArrayList<>();
        if (listeners != null) {
            Arrays.stream(listeners)
                    .filter(listener -> listener != null && listener != NoopWorkflowEventListener.INSTANCE)
                    .forEach(validListeners::add);
        }
        if (validListeners.isEmpty()) {
            return NoopWorkflowEventListener.INSTANCE;
        }
        if (validListeners.size() == 1) {
            return validListeners.get(0);
        }
        return new CompositeWorkflowEventListener(validListeners);
    }

    @Override
    public void onEvent(WorkflowExecutionEvent event) {
        for (WorkflowEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
