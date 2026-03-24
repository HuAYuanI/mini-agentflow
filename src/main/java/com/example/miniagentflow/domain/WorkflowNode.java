package com.example.miniagentflow.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 【工作流节点】：工作流中的节点   
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {

    // 【节点ID】：节点的唯一标识
    @NotBlank
    private String id;

    // 【节点类型】：节点的类型
    @NotNull
    private NodeType type;

    // 【节点配置】：节点的配置信息
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();
}
