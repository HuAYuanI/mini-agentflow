package com.example.miniagentflow.engine;

import com.example.miniagentflow.exception.WorkflowValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

// 【变量解析器】：解析节点配置中的变量引用
@Component
public class VariableResolver {

    // 【变量引用模式】：匹配 ${variable} 格式的变量引用
    private static final Pattern REF_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    // 【解析节点配置中的变量引用】：将节点配置中的变量引用替换为实际值
    public Map<String, Object> resolveMap(Map<String, Object> rawConfig, NodeExecutionContext context) {
        Map<String, Object> resolved = new HashMap<>();
        if (rawConfig == null) {
            return resolved;
        }
        // 【遍历节点配置中的所有键值对】：对每个键值对进行变量解析
        for (Map.Entry<String, Object> entry : rawConfig.entrySet()) {
            resolved.put(entry.getKey(), resolveObject(entry.getValue(), context));
        }
        return resolved;
    }

    // 【收集变量引用】：收集节点配置中的变量引用
    public Set<String> collectReferences(Map<String, Object> rawConfig) {
        Set<String> refs = new HashSet<>();
        if (rawConfig == null) {
            return refs;
        }
        // 【遍历节点配置中的所有值】：对每个值进行变量引用收集
        for (Object value : rawConfig.values()) {
            collectReferencesFromValue(value, refs);
        }
        return refs;
    }

    // 【解析对象中的变量引用】：解析对象中的变量引用
    private Object resolveObject(Object value, NodeExecutionContext context) {
        if (value == null) {
            return null;
        }
        // 【处理String】：解析字符串中的变量引用
        if (value instanceof String stringValue) {
            return resolveString(stringValue, context);
        }
        // 【处理Map】：递归解析Map中的每个值
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> resolved = new HashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                resolved.put(String.valueOf(entry.getKey()), resolveObject(entry.getValue(), context));
            }
            return resolved;
        }
        // 【处理List】：递归解析List中的每个元素
        if (value instanceof List<?> listValue) {
            List<Object> resolved = new ArrayList<>();
            for (Object item : listValue) {
                resolved.add(resolveObject(item, context));
            }
            return resolved;
        }
        // 【其他类型】：直接返回
        return value;
    }

    // 【解析字符串中的变量引用】：解析字符串中的变量引用
    private String resolveString(String value, NodeExecutionContext context) {
        Matcher matcher = REF_PATTERN.matcher(value);
        // 【判断是否有变量引用】：如果没有变量引用，直接返回原字符串
        if (!matcher.find()) {
            return value;
        }
        matcher.reset();
        // 【判断是否为单个变量引用】：如果是单个变量引用，直接返回变量值
        if (isSingleReference(value)) {
            String key = extractSingleReferenceKey(value);
            Object objectValue = requiredVariable(key, context);
            return objectValue == null ? null : String.valueOf(objectValue);
        }
        // 【处理多个变量引用】：遍历所有变量引用并替换
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object objectValue = requiredVariable(key, context);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(Objects.toString(objectValue, "")));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    // 【收集变量引用】：收集变量引用
    private void collectReferencesFromValue(Object value, Set<String> refs) {
        if (value == null) {
            return;
        }
        // 【处理String】：收集字符串中的变量引用
        if (value instanceof String stringValue) {
            Matcher matcher = REF_PATTERN.matcher(stringValue);
            while (matcher.find()) {
                refs.add(matcher.group(1).trim());
            }
            return;
        }
        // 【处理Map】：递归收集Map中的变量引用
        if (value instanceof Map<?, ?> mapValue) {
            for (Object mapEntryValue : mapValue.values()) {
                collectReferencesFromValue(mapEntryValue, refs);
            }
            return;
        }
        // 【处理List】：递归收集List中的变量引用
        if (value instanceof List<?> listValue) {
            for (Object item : listValue) {
                collectReferencesFromValue(item, refs);
            }
        }
    }

    // 【判断是否为单个变量引用】：判断字符串是否为单个变量引用
    private boolean isSingleReference(String value) {
        Matcher matcher = REF_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return false;
        }
        return value.trim().startsWith("${") && value.trim().endsWith("}");
    }

    // 【提取单个变量引用】：提取单个变量引用
    private String extractSingleReferenceKey(String value) {
        Matcher matcher = REF_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        return matcher.group(1).trim();
    }

    // 【获取变量】：获取变量
    private Object requiredVariable(String key, NodeExecutionContext context) {
        if (!context.containsVariable(key)) {
            throw new WorkflowValidationException("Unknown variable reference: " + key);
        }
        return context.getVariable(key);
    }
}
