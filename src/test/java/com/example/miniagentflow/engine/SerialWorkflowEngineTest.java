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
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void shouldRetryNodeExecutionAndEventuallySucceed() {
        VariableResolver variableResolver = new VariableResolver();
        AtomicInteger attempts = new AtomicInteger(0);
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(
                new StartNodeExecutor(variableResolver),
                new FlakyLlmNodeExecutor(variableResolver, attempts, 2),
                new EndNodeExecutor(variableResolver)
        ));
        SerialWorkflowEngine engine = new SerialWorkflowEngine(new WorkflowValidator(variableResolver), registry);

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "raw")).build(),
                        WorkflowNode.builder().id("llm").type(NodeType.LLM)
                                .config(Map.of("prompt", "retry:${raw}", "outputKey", "a", "retryTimes", 2)).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).config(Map.of("result", "${a}")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llm").build(),
                        WorkflowEdge.builder().from("llm").to("end").build()
                ))
                .build();

        WorkflowRunResult result = engine.run(workflow, Map.of("input", "hello"));
        Assertions.assertEquals("SUCCESS", result.getStatus());
        Assertions.assertEquals(3, attempts.get());
        Assertions.assertTrue(String.valueOf(result.getContextSnapshot().get("finalOutput"))
                .contains("LLM_RESPONSE: retry:hello"));
    }

    @Test
    void shouldFailFastWhenNodeTimeoutAndInterruptStrategy() {
        VariableResolver variableResolver = new VariableResolver();
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(
                new StartNodeExecutor(variableResolver),
                new LlmNodeExecutor(variableResolver),
                new EndNodeExecutor(variableResolver)
        ));
        SerialWorkflowEngine engine = new SerialWorkflowEngine(new WorkflowValidator(variableResolver), registry);

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "raw")).build(),
                        WorkflowNode.builder().id("llm").type(NodeType.LLM)
                                .config(Map.of("prompt", "slow:${raw}", "outputKey", "a", "delayMs", 250,
                                        "timeoutMs", 80, "errorStrategy", "INTERRUPT")).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END).config(Map.of("result", "${a}")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llm").build(),
                        WorkflowEdge.builder().from("llm").to("end").build()
                ))
                .build();

        WorkflowRunResult result = engine.run(workflow, Map.of("input", "hello"));
        Assertions.assertEquals("FAILED", result.getStatus());
        Assertions.assertTrue(result.getNodeResults().stream()
                .filter(nodeRunResult -> "llm".equals(nodeRunResult.getNodeId()))
                .findFirst()
                .orElseThrow()
                .getErrorMessage()
                .contains("timeout"));
    }

    @Test
    void shouldContinueToErrorBranchWhenStrategyIsErrorBranch() {
        VariableResolver variableResolver = new VariableResolver();
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(
                new StartNodeExecutor(variableResolver),
                new AlwaysFailLlmNodeExecutor(variableResolver),
                new EndNodeExecutor(variableResolver)
        ));
        SerialWorkflowEngine engine = new SerialWorkflowEngine(new WorkflowValidator(variableResolver), registry);

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .nodes(List.of(
                        WorkflowNode.builder().id("start").type(NodeType.START).config(Map.of("outputKey", "raw")).build(),
                        WorkflowNode.builder().id("llm").type(NodeType.LLM)
                                .config(Map.of("prompt", "x:${raw}", "outputKey", "a", "errorStrategy", "ERROR_BRANCH",
                                        "errorNext", "end")).build(),
                        WorkflowNode.builder().id("end").type(NodeType.END)
                                .config(Map.of("result", "fallback-result")).build()
                ))
                .edges(List.of(
                        WorkflowEdge.builder().from("start").to("llm").build(),
                        WorkflowEdge.builder().from("llm").to("end").build()
                ))
                .build();

        WorkflowRunResult result = engine.run(workflow, Map.of("input", "hello"));
        Assertions.assertEquals("PARTIAL_SUCCESS", result.getStatus());
        Assertions.assertTrue(String.valueOf(result.getContextSnapshot().get("finalOutput")).contains("fallback-result"));
        Assertions.assertEquals("llm", String.valueOf(result.getContextSnapshot().get("lastErrorNode")));
    }

    private static class FlakyLlmNodeExecutor extends LlmNodeExecutor {

        private final AtomicInteger attempts;
        private final int failBeforeSuccess;

        FlakyLlmNodeExecutor(VariableResolver variableResolver, AtomicInteger attempts, int failBeforeSuccess) {
            super(variableResolver);
            this.attempts = attempts;
            this.failBeforeSuccess = failBeforeSuccess;
        }

        @Override
        protected Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node) {
            int currentAttempt = attempts.incrementAndGet();
            if (currentAttempt <= failBeforeSuccess) {
                throw new RuntimeException("mock failure at attempt " + currentAttempt);
            }
            return super.doExecute(context, node);
        }
    }

    private static class AlwaysFailLlmNodeExecutor extends LlmNodeExecutor {

        AlwaysFailLlmNodeExecutor(VariableResolver variableResolver) {
            super(variableResolver);
        }

        @Override
        protected Map<String, Object> doExecute(NodeExecutionContext context, WorkflowNode node) {
            throw new RuntimeException("forced failure");
        }
    }
}
