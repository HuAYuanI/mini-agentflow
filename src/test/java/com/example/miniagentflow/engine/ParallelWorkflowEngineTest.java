package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.NodeType;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowEdge;
import com.example.miniagentflow.domain.WorkflowEventType;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.domain.WorkflowRunResult;
import com.example.miniagentflow.engine.executor.EndNodeExecutor;
import com.example.miniagentflow.engine.executor.LlmNodeExecutor;
import com.example.miniagentflow.engine.executor.PluginNodeExecutor;
import com.example.miniagentflow.engine.executor.StartNodeExecutor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParallelWorkflowEngineTest {

    @Test
    void shouldRunParallelWorkflowForIndependentNodes() {
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
                        WorkflowNode.builder().id("llmA").type(NodeType.LLM).config(Map.of("prompt", "A:${raw}", "outputKey", "a", "delayMs", 500)).build(),
                        WorkflowNode.builder().id("llmB").type(NodeType.LLM).config(Map.of("prompt", "B:${raw}", "outputKey", "b", "delayMs", 500)).build(),
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

        WorkflowRunResult serialResult = serialWorkflowEngine.run(workflow, inputs);
        WorkflowRunResult parallelResult = parallelWorkflowEngine.run(workflow, inputs);

        Assertions.assertEquals("SUCCESS", serialResult.getStatus());
        Assertions.assertEquals("SUCCESS", parallelResult.getStatus());
        Assertions.assertEquals(serialResult.getNodeResults().size(), parallelResult.getNodeResults().size());
        Assertions.assertEquals(WorkflowEventType.WORKFLOW_STARTED, parallelResult.getEvents().getFirst().getType());
        Assertions.assertEquals(WorkflowEventType.WORKFLOW_COMPLETED, parallelResult.getEvents().getLast().getType());
        Assertions.assertTrue(String.valueOf(parallelResult.getContextSnapshot().get("finalOutput")).contains("PLUGIN_OK"));
    }

    @Test
    void shouldPropagateExecutionIdAcrossParallelWorkers() {
        VariableResolver variableResolver = new VariableResolver();
        Set<String> executionIds = ConcurrentHashMap.newKeySet();
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(
                new StartNodeExecutor(variableResolver),
                new TtlAwareLlmNodeExecutor(variableResolver, executionIds),
                new EndNodeExecutor(variableResolver)
        ));
        WorkflowValidator validator = new WorkflowValidator(variableResolver);
        ParallelWorkflowEngine parallelWorkflowEngine = new ParallelWorkflowEngine(validator, registry, 4);

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "raw")).build(),
                        WorkflowNode.builder().id("llmA").type(NodeType.LLM)
                                .config(Map.of("prompt", "A:${raw}", "outputKey", "a", "delayMs", 200)).build(),
                        WorkflowNode.builder().id("llmB").type(NodeType.LLM)
                                .config(Map.of("prompt", "B:${raw}", "outputKey", "b", "delayMs", 200)).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END)
                                .config(Map.of("result", "${a}-${b}")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llmA").build(),
                        WorkflowEdge.builder().from("start").to("llmB").build(),
                        WorkflowEdge.builder().from("llmA").to("end").build(),
                        WorkflowEdge.builder().from("llmB").to("end").build()
                ))
                .build();

        WorkflowRunResult result = parallelWorkflowEngine.run(workflow, Map.of("input", "hello"));
        Assertions.assertEquals("SUCCESS", result.getStatus());
        Assertions.assertEquals(1, executionIds.size());
        Assertions.assertEquals(result.getContextSnapshot().get("executionId"), executionIds.iterator().next());
        Assertions.assertEquals(1, result.getEvents().stream().map(event -> event.getExecutionId()).distinct().count());
        Assertions.assertNull(WorkflowThreadContextHolder.get());
    }

    @Test
    void shouldContinueWhenFailureStrategyIsContinueInParallelEngine() {
        VariableResolver variableResolver = new VariableResolver();
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(
                new StartNodeExecutor(variableResolver),
                new FailOnNodeLlmExecutor(variableResolver, Set.of("llmFail")),
                new EndNodeExecutor(variableResolver)
        ));
        WorkflowValidator validator = new WorkflowValidator(variableResolver);
        ParallelWorkflowEngine parallelWorkflowEngine = new ParallelWorkflowEngine(validator, registry, 4);

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "raw")).build(),
                        WorkflowNode.builder().id("llmFail").type(NodeType.LLM)
                                .config(Map.of("prompt", "X:${raw}", "outputKey", "a", "errorStrategy", "CONTINUE")).build(),
                        WorkflowNode.builder().id("llmOk").type(NodeType.LLM)
                                .config(Map.of("prompt", "Y:${raw}", "outputKey", "b")).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END)
                                .config(Map.of("result", "${b}")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llmFail").build(),
                        WorkflowEdge.builder().from("start").to("llmOk").build(),
                        WorkflowEdge.builder().from("llmFail").to("end").build(),
                        WorkflowEdge.builder().from("llmOk").to("end").build()
                ))
                .build();

        WorkflowRunResult result = parallelWorkflowEngine.run(workflow, Map.of("input", "hello"));
        Assertions.assertEquals("PARTIAL_SUCCESS", result.getStatus());
        Assertions.assertTrue(result.getNodeResults().stream()
                .anyMatch(nodeRunResult -> "llmFail".equals(nodeRunResult.getNodeId())
                        && nodeRunResult.getStatus().name().equals("FAILED")));
        Assertions.assertEquals("PARTIAL_SUCCESS", result.getEvents().getLast().getWorkflowStatus());
        Assertions.assertTrue(String.valueOf(result.getContextSnapshot().get("finalOutput"))
                .contains("LLM_RESPONSE: Y:hello"));
    }

    private static class TtlAwareLlmNodeExecutor extends LlmNodeExecutor {

        private final Set<String> executionIds;

        TtlAwareLlmNodeExecutor(VariableResolver variableResolver, Set<String> executionIds) {
            super(variableResolver);
            this.executionIds = executionIds;
        }

        @Override
        protected Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node) {
            WorkflowThreadContext threadContext = WorkflowThreadContextHolder.get();
            Assertions.assertNotNull(threadContext);
            Assertions.assertNotNull(threadContext.getExecutionId());
            executionIds.add(threadContext.getExecutionId());

            Map<String, Object> output = new HashMap<>();
            Map<String, Object> config = node.getConfig();
            String outputKey = String.valueOf(config.getOrDefault("outputKey", "llmOutput"));
            String prompt = String.valueOf(config.getOrDefault("prompt", "default"));
            output.put(outputKey, "MOCK_LLM:" + prompt);
            return output;
        }
    }

    private static class FailOnNodeLlmExecutor extends LlmNodeExecutor {

        private final Set<String> failNodeIds;

        FailOnNodeLlmExecutor(VariableResolver variableResolver, Set<String> failNodeIds) {
            super(variableResolver);
            this.failNodeIds = failNodeIds;
        }

        @Override
        protected Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node) {
            if (failNodeIds.contains(node.getId())) {
                throw new RuntimeException("forced failure for " + node.getId());
            }
            return super.doExecute(context, node);
        }
    }
}
