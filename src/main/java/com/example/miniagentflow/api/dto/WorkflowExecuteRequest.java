package com.example.miniagentflow.api.dto;

import com.example.miniagentflow.domain.EngineMode;
import com.example.miniagentflow.domain.WorkflowDefinition;
import jakarta.validation.Valid;
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
public class WorkflowExecuteRequest {

    @Valid
    @NotNull
    private WorkflowDefinition workflow;

    @Builder.Default
    private Map<String, Object> inputs = new HashMap<>();

    @Builder.Default
    private EngineMode engineMode = EngineMode.SERIAL;
}
