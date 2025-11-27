package com.poli.chatbot.repo;

import com.poli.chatbot.model.ActionEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActionEventRepository extends MongoRepository<ActionEvent, String> {
    List<ActionEvent> findTop50BySessionIdOrderByTsDesc(String sessionId);
}
