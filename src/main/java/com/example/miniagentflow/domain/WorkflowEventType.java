package com.example.miniagentflow.domain;

public enum WorkflowEventType {
    WORKFLOW_STARTED,
    NODE_STARTED,
    NODE_RETRYING,
    NODE_COMPLETED,
    NODE_FAILED,
    WORKFLOW_COMPLETED
}
