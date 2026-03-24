package com.example.miniagentflow.engine.event;

import com.example.miniagentflow.domain.WorkflowExecutionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CollectingWorkflowEventListener implements WorkflowEventListener {

    private final List<WorkflowExecutionEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(WorkflowExecutionEvent event) {
        events.add(event);
    }

    public List<WorkflowExecutionEvent> snapshot() {
        return new ArrayList<>(events);
    }
}
