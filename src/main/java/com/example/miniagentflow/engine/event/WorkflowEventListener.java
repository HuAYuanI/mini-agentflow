package com.example.miniagentflow.engine.event;

import com.example.miniagentflow.domain.WorkflowExecutionEvent;

public interface WorkflowEventListener {

    void onEvent(WorkflowExecutionEvent event);
}
