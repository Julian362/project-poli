package com.poli.chatbot.repo;

import com.poli.chatbot.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
}
