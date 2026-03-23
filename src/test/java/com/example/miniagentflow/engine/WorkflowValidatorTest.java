package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeType;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowEdge;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.exception.WorkflowValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WorkflowValidatorTest {

    private final VariableResolver variableResolver = new VariableResolver();
    private final WorkflowValidator workflowValidator = new WorkflowValidator(variableResolver);

    @Test
    void shouldDetectCycle() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("n1").type(NodeType.START).build(),
                        WorkflowNode.builder().id("n2").type(NodeType.LLM).build(),
                        WorkflowNode.builder().id("n3").type(NodeType.END).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("n1").to("n2").build(),
                        WorkflowEdge.builder().from("n2").to("n3").build(),
                        WorkflowEdge.builder().from("n3").to("n2").build()
                ))
                .build();

        Assertions.assertThrows(WorkflowValidationException.class, () -> workflowValidator.validateAndSort(workflow));
    }

    @Test
    void shouldReturnTopologicalOrder() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).build(),
                        WorkflowNode.builder().id("llm").type(NodeType.LLM).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llm").build(),
                        WorkflowEdge.builder().from("llm").to("end").build()
                ))
                .build();

        List<String> order = workflowValidator.validateAndSort(workflow);
        Assertions.assertEquals(3, order.size());
        Assertions.assertEquals("start", order.get(0));
        Assertions.assertEquals("end", order.get(2));
    }

    @Test
    void shouldRejectUnknownVariableReference() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).build(),
                        WorkflowNode.builder().id("llm").type(NodeType.LLM).config(Map.of("prompt", "${missingVar}")).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llm").build(),
                        WorkflowEdge.builder().from("llm").to("end").build()
                ))
                .build();

        Assertions.assertThrows(WorkflowValidationException.class, () -> workflowValidator.validateAndSort(workflow, Map.of("input", "hello")));
    }

    @Test
    void shouldRejectSiblingVariableReference() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "raw")).build(),
                        WorkflowNode.builder().id("llmA").type(NodeType.LLM).config(Map.of("prompt", "${b}", "outputKey", "a")).build(),
                        WorkflowNode.builder().id("llmB").type(NodeType.LLM).config(Map.of("prompt", "${raw}", "outputKey", "b")).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).config(Map.of("result", "${a}")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llmA").build(),
                        WorkflowEdge.builder().from("start").to("llmB").build(),
                        WorkflowEdge.builder().from("llmA").to("end").build(),
                        WorkflowEdge.builder().from("llmB").to("end").build()
                ))
                .build();

        Assertions.assertThrows(WorkflowValidationException.class, () -> workflowValidator.validateAndSort(workflow, Map.of("input", "hello")));
    }

    @Test
    void shouldRejectErrorBranchStrategyWithoutErrorNext() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "raw")).build(),
                        WorkflowNode.builder().id("llm").type(NodeType.LLM)
                                .config(Map.of("prompt", "${raw}", "outputKey", "a", "errorStrategy", "ERROR_BRANCH")).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).config(Map.of("result", "${a}")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llm").build(),
                        WorkflowEdge.builder().from("llm").to("end").build()
                ))
                .build();

        Assertions.assertThrows(WorkflowValidationException.class,
                () -> workflowValidator.validateAndSort(workflow, Map.of("input", "hello")));
    }
}
