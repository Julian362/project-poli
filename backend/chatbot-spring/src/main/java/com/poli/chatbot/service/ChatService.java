package com.poli.chatbot.service;

import com.poli.chatbot.model.ChatMessage;
import com.poli.chatbot.model.ChatSession;
import com.poli.chatbot.repo.ChatMessageRepository;
import com.poli.chatbot.repo.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;
    private final DeepSeekClient deepSeekClient;

    public ChatService(ChatMessageRepository messageRepository,
                       ChatSessionRepository sessionRepository,
                       DeepSeekClient deepSeekClient) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.deepSeekClient = deepSeekClient;
    }

    public ChatSession getOrCreateSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseGet(() -> sessionRepository.save(new ChatSession(sessionId, Instant.now(), "active")));
    }

    public ChatMessage handleUserMessage(String sessionId, String question) {
        ChatSession session = getOrCreateSession(sessionId);
        // Save user question
        ChatMessage userMsg = ChatMessage.user(sessionId, question);
        messageRepository.save(userMsg);
        // Call DeepSeek
        String answer = deepSeekClient.complete(question, sessionId);
        // Save assistant reply
        ChatMessage assistantMsg = ChatMessage.assistant(sessionId, answer);
        messageRepository.save(assistantMsg);
        // Update session state if needed
        session.setLastUpdated(Instant.now());
        sessionRepository.save(session);
        return assistantMsg;
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return messageRepository.findBySessionIdOrderByTsAsc(sessionId);
    }
}
