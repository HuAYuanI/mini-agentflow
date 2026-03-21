package com.example.miniagentflow.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunResult {

    private String status;

    @Builder.Default
    private List<NodeRunResult> nodeResults = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> contextSnapshot = new HashMap<>();
}
