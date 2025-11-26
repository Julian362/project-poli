package com.poli.chatbot.repo;

import com.poli.chatbot.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findBySessionIdOrderByTsAsc(String sessionId);
}
