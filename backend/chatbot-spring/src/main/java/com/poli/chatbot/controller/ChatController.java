package com.poli.chatbot.controller;

import com.poli.chatbot.model.ChatMessage;
import com.poli.chatbot.model.ChatSession;
import com.poli.chatbot.service.ChatService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatMessage> ask(@RequestParam @NotBlank String sessionId,
                                           @RequestParam @NotBlank String question) {
        ChatMessage reply = chatService.handleUserMessage(sessionId, question);
        return ResponseEntity.ok(reply);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> history(@RequestParam @NotBlank String sessionId) {
        return ResponseEntity.ok(chatService.getHistory(sessionId));
    }

    @GetMapping("/session")
    public ResponseEntity<ChatSession> session(@RequestParam @NotBlank String sessionId) {
        return ResponseEntity.ok(chatService.getOrCreateSession(sessionId));
    }
}
