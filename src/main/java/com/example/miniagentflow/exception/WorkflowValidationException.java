package com.example.miniagentflow.exception;

// 【工作流验证异常】：工作流验证失败时抛出的异常
public class WorkflowValidationException extends RuntimeException {

    // 构造函数
    public WorkflowValidationException(String message) {
        super(message);
    }
}
