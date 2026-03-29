package com.example.miniagentflow.ai;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "miniagentflow.ai")
public class AgentflowAiProperties {

    private boolean mockEnabled = true;
    private String defaultSystemPrompt = "你是一个擅长 Java 后端、工作流和 Agent 系统设计的助手。";

    @Min(1)
    private int memoryWindow = 8;
}
