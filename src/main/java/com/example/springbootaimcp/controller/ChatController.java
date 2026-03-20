package com.example.springbootaimcp.controller;

import com.example.springbootaimcp.agent.AgentService;
import com.example.springbootaimcp.dto.ChatRequest;
import com.example.springbootaimcp.dto.ChatResponse;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public CompletableFuture<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到聊天请求, conversationId={}, messageLength={}",
                request.getConversationId(), request.getMessage() == null ? 0 : request.getMessage().length());
        return agentService.dispatch(request);
    }
}
