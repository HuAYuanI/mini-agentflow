package com.example.miniagentflow.api;

import com.example.miniagentflow.api.dto.ChatCompletionRequest;
import com.example.miniagentflow.api.dto.ChatCompletionResponse;
import com.example.miniagentflow.service.AgentChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AgentChatService agentChatService;

    public ChatController(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    @PostMapping("/complete")
    public ChatCompletionResponse complete(@Valid @RequestBody ChatCompletionRequest request) {
        return agentChatService.complete(request);
    }
}
