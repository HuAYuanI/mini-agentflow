package com.example.miniagentflow.domain;

// 【错误策略枚举】：定义节点执行失败时的处理策略
public enum ErrorStrategyEnum {
    INTERRUPT, // 中断：节点执行失败时，中断整个工作流
    CONTINUE, // 继续：节点执行失败时，继续执行后续节点
    ERROR_BRANCH // 错误分支：节点执行失败时，执行错误分支
}
