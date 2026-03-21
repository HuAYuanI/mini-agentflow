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
public class NodeRunResult {

    private String nodeId;
    private NodeRunStatus status;

    @Builder.Default
    private Map<String, Object> output = new HashMap<>();

    private String errorMessage;

    public static NodeRunResult success(String nodeId, Map<String, Object> output) {
        return NodeRunResult.builder()
                .nodeId(nodeId)
                .status(NodeRunStatus.SUCCESS)
                .output(output == null ? new HashMap<>() : output)
                .build();
    }

    public static NodeRunResult failed(String nodeId, String errorMessage) {
        return NodeRunResult.builder()
                .nodeId(nodeId)
                .status(NodeRunStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
}
