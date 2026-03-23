package com.example.miniagentflow.engine;

import com.example.miniagentflow.domain.ErrorStrategyEnum;
import com.example.miniagentflow.domain.NodeType;
import com.example.miniagentflow.domain.WorkflowDefinition;
import com.example.miniagentflow.domain.WorkflowEdge;
import com.example.miniagentflow.domain.WorkflowNode;
import com.example.miniagentflow.exception.WorkflowValidationException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class WorkflowValidator {

    private final VariableResolver variableResolver;

    public WorkflowValidator(VariableResolver variableResolver) {
        this.variableResolver = variableResolver;
    }

    public List<String> validateAndSort(WorkflowDefinition workflow) {
        return validateAndSort(workflow, Collections.emptyMap());
    }

    public List<String> validateAndSort(WorkflowDefinition workflow, Map<String, Object> inputs) {
        if (workflow == null || workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            throw new WorkflowValidationException("Workflow nodes must not be empty");
        }
        List<WorkflowNode> nodes = workflow.getNodes();
        List<WorkflowEdge> edges = Objects.requireNonNullElseGet(workflow.getEdges(), ArrayList::new);

        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNode node : nodes) {
            if (nodeMap.containsKey(node.getId())) {
                throw new WorkflowValidationException("Duplicate node id: " + node.getId());
            }
            nodeMap.put(node.getId(), node);
        }

        long startCount = nodes.stream().filter(n -> n.getType() == NodeType.START).count();
        long endCount = nodes.stream().filter(n -> n.getType() == NodeType.END).count();
        if (startCount != 1) {
            throw new WorkflowValidationException("Workflow must contain exactly one START node");
        }
        if (endCount != 1) {
            throw new WorkflowValidationException("Workflow must contain exactly one END node");
        }

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, List<String>> reverseAdjacency = new HashMap<>();
        for (WorkflowNode node : nodes) {
            inDegree.put(node.getId(), 0);
            outDegree.put(node.getId(), 0);
            adjacency.put(node.getId(), new ArrayList<>());
            reverseAdjacency.put(node.getId(), new ArrayList<>());
        }

        Set<String> edgeSet = new HashSet<>();
        for (WorkflowEdge edge : edges) {
            if (Objects.equals(edge.getFrom(), edge.getTo())) {
                throw new WorkflowValidationException("Self-loop is not allowed: " + edge.getFrom());
            }
            if (!nodeMap.containsKey(edge.getFrom()) || !nodeMap.containsKey(edge.getTo())) {
                throw new WorkflowValidationException("Edge contains unknown node: " + edge);
            }
            String edgeKey = edge.getFrom() + "->" + edge.getTo();
            if (!edgeSet.add(edgeKey)) {
                throw new WorkflowValidationException("Duplicate edge: " + edgeKey);
            }
            adjacency.get(edge.getFrom()).add(edge.getTo());
            reverseAdjacency.get(edge.getTo()).add(edge.getFrom());
            outDegree.put(edge.getFrom(), outDegree.get(edge.getFrom()) + 1);
            inDegree.put(edge.getTo(), inDegree.get(edge.getTo()) + 1);
        }
        Map<String, Integer> originalInDegree = new HashMap<>(inDegree);

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<String> topologicalOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            topologicalOrder.add(current);
            for (String next : adjacency.get(current)) {
                int value = inDegree.get(next) - 1;
                inDegree.put(next, value);
                if (value == 0) {
                    queue.offer(next);
                }
            }
        }

        if (topologicalOrder.size() != nodes.size()) {
            throw new WorkflowValidationException("Workflow contains cycle");
        }

        ensureNodeTypeGraphConstraints(nodeMap, originalInDegree, outDegree);
        ensureReachability(nodeMap, adjacency, reverseAdjacency);
        validateVariableReferences(nodeMap, topologicalOrder, reverseAdjacency, inputs);
        validateErrorStrategies(nodeMap, adjacency);
        return topologicalOrder;
    }

    private void ensureNodeTypeGraphConstraints(Map<String, WorkflowNode> nodeMap,
                                                Map<String, Integer> inDegree,
                                                Map<String, Integer> outDegree) {
        for (WorkflowNode node : nodeMap.values()) {
            if (node.getType() == NodeType.START && inDegree.get(node.getId()) != 0) {
                throw new WorkflowValidationException("START node in-degree must be 0: " + node.getId());
            }
            if (node.getType() == NodeType.END && outDegree.get(node.getId()) != 0) {
                throw new WorkflowValidationException("END node out-degree must be 0: " + node.getId());
            }
        }
    }

    private void ensureReachability(Map<String, WorkflowNode> nodeMap,
                                    Map<String, List<String>> adjacency,
                                    Map<String, List<String>> reverseAdjacency) {
        String startNodeId = nodeMap.values().stream().filter(n -> n.getType() == NodeType.START).findFirst()
                .orElseThrow(() -> new WorkflowValidationException("START node missing")).getId();
        String endNodeId = nodeMap.values().stream().filter(n -> n.getType() == NodeType.END).findFirst()
                .orElseThrow(() -> new WorkflowValidationException("END node missing")).getId();

        Set<String> reachableFromStart = bfs(startNodeId, adjacency);
        if (reachableFromStart.size() != nodeMap.size()) {
            throw new WorkflowValidationException("Workflow has unreachable nodes from START");
        }
        Set<String> canReachEnd = bfs(endNodeId, reverseAdjacency);
        if (canReachEnd.size() != nodeMap.size()) {
            throw new WorkflowValidationException("Workflow has dead-end path that cannot reach END");
        }
    }

    private Set<String> bfs(String root, Map<String, List<String>> adjacency) {
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.offer(root);
        visited.add(root);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String next : adjacency.get(current)) {
                if (visited.add(next)) {
                    queue.offer(next);
                }
            }
        }
        return visited;
    }

    private void validateVariableReferences(Map<String, WorkflowNode> nodeMap,
                                            List<String> topologicalOrder,
                                            Map<String, List<String>> reverseAdjacency,
                                            Map<String, Object> inputs) {
        Map<String, String> nodeOutputKeyMap = new HashMap<>();
        Set<String> outputKeySet = new HashSet<>();
        for (String nodeId : topologicalOrder) {
            String outputKey = resolveOutputKey(nodeMap.get(nodeId));
            if (!outputKeySet.add(outputKey)) {
                throw new WorkflowValidationException("Duplicate output key is not allowed: " + outputKey);
            }
            nodeOutputKeyMap.put(nodeId, outputKey);
        }

        Set<String> inputVariables = new HashSet<>();
        if (inputs != null) {
            inputVariables.addAll(inputs.keySet());
        }
        inputVariables.add("input");

        for (String nodeId : topologicalOrder) {
            WorkflowNode node = nodeMap.get(nodeId);
            Set<String> allowedVariables = new HashSet<>(inputVariables);
            for (String ancestor : bfs(nodeId, reverseAdjacency)) {
                if (!ancestor.equals(nodeId)) {
                    allowedVariables.add(nodeOutputKeyMap.get(ancestor));
                }
            }
            Set<String> refs = variableResolver.collectReferences(node.getConfig());
            for (String ref : refs) {
                if (!allowedVariables.contains(ref)) {
                    throw new WorkflowValidationException("Node " + nodeId + " references unknown variable: " + ref);
                }
            }
        }
    }

    private String resolveOutputKey(WorkflowNode node) {
        Object outputKey = node.getConfig().get("outputKey");
        if (outputKey instanceof String outputKeyString
                && !outputKeyString.isBlank()
                && !outputKeyString.contains("${")) {
            return outputKeyString;
        }
        return switch (node.getType()) {
            case START -> "startOutput";
            case LLM -> "llmOutput";
            case PLUGIN -> "pluginOutput";
            case END -> "finalOutput";
        };
    }

    private void validateErrorStrategies(Map<String, WorkflowNode> nodeMap, Map<String, List<String>> adjacency) {
        for (WorkflowNode node : nodeMap.values()) {
            ErrorStrategyEnum strategy = parseStrategy(node.getConfig().get("errorStrategy"));
            if (strategy != ErrorStrategyEnum.ERROR_BRANCH) {
                continue;
            }
            Object errorNextObj = node.getConfig().get("errorNext");
            if (!(errorNextObj instanceof String errorNext) || errorNext.isBlank()) {
                throw new WorkflowValidationException("Node " + node.getId() + " missing errorNext for ERROR_BRANCH");
            }
            if (!nodeMap.containsKey(errorNext)) {
                throw new WorkflowValidationException("Node " + node.getId() + " errorNext not found: " + errorNext);
            }
            if (!adjacency.get(node.getId()).contains(errorNext)) {
                throw new WorkflowValidationException(
                        "Node " + node.getId() + " errorNext must be a direct downstream node: " + errorNext);
            }
        }
    }

    private ErrorStrategyEnum parseStrategy(Object strategyObj) {
        if (strategyObj == null) {
            return ErrorStrategyEnum.INTERRUPT;
        }
        String raw = String.valueOf(strategyObj).trim();
        if (raw.isEmpty()) {
            return ErrorStrategyEnum.INTERRUPT;
        }
        try {
            return ErrorStrategyEnum.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new WorkflowValidationException("Unknown errorStrategy: " + raw);
        }
    }
}
