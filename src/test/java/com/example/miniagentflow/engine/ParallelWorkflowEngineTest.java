package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeType;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowEdge;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.executor.EndNodeExecutor;
import com.example.miniagentflow.engine.executor.LlmNodeExecutor;
import com.example.miniagentflow.engine.executor.PluginNodeExecutor;
import com.example.miniagentflow.engine.executor.StartNodeExecutor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParallelWorkflowEngineTest {

    @Test
    void shouldRunParallelFasterThanSerialForIndependentNodes() {
        VariableResolver variableResolver = new VariableResolver();
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(
                new StartNodeExecutor(variableResolver),
                new LlmNodeExecutor(variableResolver),
                new PluginNodeExecutor(variableResolver),
                new EndNodeExecutor(variableResolver)
        ));
        WorkflowValidator validator = new WorkflowValidator(variableResolver);
        SerialWorkflowEngine serialWorkflowEngine = new SerialWorkflowEngine(validator, registry);
        ParallelWorkflowEngine parallelWorkflowEngine = new ParallelWorkflowEngine(validator, registry, 4);

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "raw")).build(),
                        WorkflowNode.builder().id("llmA").type(NodeType.LLM).config(Map.of("prompt", "A:${raw}", "outputKey", "a", "delayMs", 350)).build(),
                        WorkflowNode.builder().id("llmB").type(NodeType.LLM).config(Map.of("prompt", "B:${raw}", "outputKey", "b", "delayMs", 350)).build(),
                        WorkflowNode.builder().id("plugin").type(NodeType.PLUGIN).config(Map.of("text", "${a}|${b}", "outputKey", "merge")).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).config(Map.of("result", "${merge}")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llmA").build(),
                        WorkflowEdge.builder().from("start").to("llmB").build(),
                        WorkflowEdge.builder().from("llmA").to("plugin").build(),
                        WorkflowEdge.builder().from("llmB").to("plugin").build(),
                        WorkflowEdge.builder().from("plugin").to("end").build()
                ))
                .build();

        Map<String, Object> inputs = Map.of("input", "hello");

        long serialStart = System.currentTimeMillis();
        WorkflowRunResult serialResult = serialWorkflowEngine.run(workflow, inputs);
        long serialElapsed = System.currentTimeMillis() - serialStart;

        long parallelStart = System.currentTimeMillis();
        WorkflowRunResult parallelResult = parallelWorkflowEngine.run(workflow, inputs);
        long parallelElapsed = System.currentTimeMillis() - parallelStart;

        Assertions.assertEquals("SUCCESS", serialResult.getStatus());
        Assertions.assertEquals("SUCCESS", parallelResult.getStatus());
        Assertions.assertTrue(parallelElapsed + 120 < serialElapsed);
        Assertions.assertTrue(String.valueOf(parallelResult.getContextSnapshot().get("finalOutput")).contains("PLUGIN_OK"));
    }
}
