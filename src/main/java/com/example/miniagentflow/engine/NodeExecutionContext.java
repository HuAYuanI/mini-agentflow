package com.example.miniagentflow.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

// 【执行上下文】：伴随整条流水线的“全局记事本”
@Getter
public class NodeExecutionContext {

    // 【全局变量】：所有节点共享的读写变量池，用于节点间传递数据，ConcurrentHashMap保证线程安全
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    // 【节点输出缓存】：存储每个节点的输出结果，供后续节点引用，ConcurrentHashMap保证线程安全
    private final Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();

    // 构造函数：初始化时将输入参数放入变量池
    public NodeExecutionContext(Map<String, Object> inputs) {
        if (inputs != null) {
            variables.putAll(inputs);
        }
    }

    // 【写入全局变量】：节点执行后将结果写入变量池，供后续节点使用
    public void putVariable(String key, Object value) {
        variables.put(key, value);
    }

    // 【读取全局变量】：节点执行时读取变量池中的数据
    public Object getVariable(String key) {
        return variables.get(key);
    }

    // 【判断变量是否存在】：节点执行时判断变量池中的数据是否存在
    public boolean containsVariable(String key) {
        return variables.containsKey(key);
    }

    // 【写入节点输出】：节点执行后将结果存入缓存，供后续节点引用
    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        nodeOutputs.put(nodeId, output == null ? new HashMap<>() : new HashMap<>(output));
    }

    // 【获取所有变量】：获取当前上下文中的所有变量
    public Map<String, Object> snapshotVariables() {
        return new HashMap<>(variables);
    }

    // 【获取所有节点输出】：获取当前上下文中的所有节点输出
    public Map<String, Map<String, Object>> getNodeOutputs() {
        return new HashMap<>(nodeOutputs);
    }

    // 【清空上下文】：清空所有变量和节点输出，用于重新执行
    public void clear() {
        variables.clear();
        nodeOutputs.clear();
    }
}
