package com.example.miniagentflow.engine.error;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

// 【节点执行失败决策】：节点执行失败时的决策结果
@Getter
@Builder
public class NodeFailureDecision {

    // 是否中断整个工作流
    private final boolean interruptWorkflow;

    // 下一个要执行的节点ID列表
    @Builder.Default
    private final List<String> nextNodeIds = new ArrayList<>();

    // 创建中断决策
    public static NodeFailureDecision interrupt() {
        return NodeFailureDecision.builder()
                .interruptWorkflow(true) // 中断整个工作流
                .build();
    }

    // 创建继续执行决策
    public static NodeFailureDecision continueWith(List<String> nextNodeIds) {
        return NodeFailureDecision.builder()
                .interruptWorkflow(false) // 不中断整个工作流
                .nextNodeIds(nextNodeIds == null ? new ArrayList<>() : new ArrayList<>(nextNodeIds)) // 下一个要执行的节点ID列表
                .build();
    }
}
