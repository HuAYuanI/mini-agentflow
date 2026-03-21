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

class SerialWorkflowEngineTest {

    @Test
    void shouldRunLinearWorkflow() {
        VariableResolver variableResolver = new VariableResolver();
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(
                new StartNodeExecutor(variableResolver),
                new LlmNodeExecutor(variableResolver),
                new PluginNodeExecutor(variableResolver),
                new EndNodeExecutor(variableResolver)
        ));
        SerialWorkflowEngine engine = new SerialWorkflowEngine(new WorkflowValidator(variableResolver), registry);

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).build(),
                        WorkflowNode.builder().id("llm").type(NodeType.LLM).build(),
                        WorkflowNode.builder().id("plugin").type(NodeType.PLUGIN).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llm").build(),
                        WorkflowEdge.builder().from("llm").to("plugin").build(),
                        WorkflowEdge.builder().from("plugin").to("end").build()
                ))
                .build();

        WorkflowRunResult result = engine.run(workflow, Map.of("input", "hello"));
        Assertions.assertEquals("SUCCESS", result.getStatus());
        Assertions.assertEquals(4, result.getNodeResults().size());
        Assertions.assertTrue(String.valueOf(result.getContextSnapshot().get("finalOutput")).contains("PLUGIN_OK"));
    }

    @Test
    void shouldResolveVariableReferencesWithCustomOutputKey() {
        VariableResolver variableResolver = new VariableResolver();
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(
                new StartNodeExecutor(variableResolver),
                new LlmNodeExecutor(variableResolver),
                new PluginNodeExecutor(variableResolver),
                new EndNodeExecutor(variableResolver)
        ));
        SerialWorkflowEngine engine = new SerialWorkflowEngine(new WorkflowValidator(variableResolver), registry);

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "rawInput")).build(),
                        WorkflowNode.builder().id("llm").type(NodeType.LLM).config(Map.of("prompt", "改写: ${rawInput}", "outputKey", "rewritten")).build(),
                        WorkflowNode.builder().id("plugin").type(NodeType.PLUGIN).config(Map.of("text", "${rewritten}", "outputKey", "audioText")).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).config(Map.of("result", "${audioText}")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llm").build(),
                        WorkflowEdge.builder().from("llm").to("plugin").build(),
                        WorkflowEdge.builder().from("plugin").to("end").build()
                ))
                .build();

        WorkflowRunResult result = engine.run(workflow, Map.of("input", "hello"));
        Assertions.assertEquals("SUCCESS", result.getStatus());
        Assertions.assertTrue(String.valueOf(result.getContextSnapshot().get("finalOutput")).contains("PLUGIN_OK: LLM_RESPONSE: 改写: hello"));
    }
}
