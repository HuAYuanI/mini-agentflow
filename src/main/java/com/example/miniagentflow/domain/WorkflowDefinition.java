package com.example.miniagentflow.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {

    @Valid
    @NotEmpty
    @Builder.Default
    private List<WorkflowNode> nodes = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<WorkflowEdge> edges = new ArrayList<>();
}
