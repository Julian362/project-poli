package com.poli.chatbot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("chat_sessions")
public class ChatSession {
    @Id
    private String id; // sessionId
    private Instant created;
    private Instant lastUpdated;
    private String state;

    public ChatSession() {}

    public ChatSession(String id, Instant created, String state) {
        this.id = id;
        this.created = created;
        this.lastUpdated = created;
        this.state = state;
    }

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getCreated() { return created; }
    public void setCreated(Instant created) { this.created = created; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
