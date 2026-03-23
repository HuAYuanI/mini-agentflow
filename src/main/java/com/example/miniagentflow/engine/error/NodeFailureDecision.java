package com.example.miniagentflow.engine.error;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NodeFailureDecision {

    private final boolean interruptWorkflow;

    @Builder.Default
    private final List<String> nextNodeIds = new ArrayList<>();

    public static NodeFailureDecision interrupt() {
        return NodeFailureDecision.builder()
                .interruptWorkflow(true)
                .build();
    }

    public static NodeFailureDecision continueWith(List<String> nextNodeIds) {
        return NodeFailureDecision.builder()
                .interruptWorkflow(false)
                .nextNodeIds(nextNodeIds == null ? new ArrayList<>() : new ArrayList<>(nextNodeIds))
                .build();
    }
}
