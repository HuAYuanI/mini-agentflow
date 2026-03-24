package com.example.miniagentflow.engine.event;

import com.example.miniagentflow.domain.WorkflowExecutionEvent;

public enum NoopWorkflowEventListener implements WorkflowEventListener {
    INSTANCE;

    @Override
    public void onEvent(WorkflowExecutionEvent event) {
    }
}
