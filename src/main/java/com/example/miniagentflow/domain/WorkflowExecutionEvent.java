package com.example.miniagentflow.domain;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionEvent {

    private long sequence;
    private long timestamp;
    private WorkflowEventType type;
    private String executionId;
    private String engineMode;
    private String workflowStatus;
    private String nodeId;
    private String nodeType;
    private Integer attempt;
    private String message;

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
}
