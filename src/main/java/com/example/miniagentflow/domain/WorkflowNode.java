package com.example.miniagentflow.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class WorkflowNode {

    @NotBlank
    private String id;

    @NotNull
    private NodeType type;

    @Builder.Default
    private Map<String, Object> config = new HashMap<>();
}
