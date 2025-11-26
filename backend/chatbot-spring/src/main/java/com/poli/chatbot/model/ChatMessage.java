package com.poli.chatbot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("chat_messages")
public class ChatMessage {
    @Id
    private String id;
    private String sessionId;
    private String role; // user | assistant
    private String text;
    private Instant ts;

    public ChatMessage() {}

    public ChatMessage(String sessionId, String role, String text, Instant ts) {
        this.sessionId = sessionId;
        this.role = role;
        this.text = text;
        this.ts = ts;
    }

    public static ChatMessage user(String sessionId, String text) {
        return new ChatMessage(sessionId, "user", text, Instant.now());
    }

    public static ChatMessage assistant(String sessionId, String text) {
        return new ChatMessage(sessionId, "assistant", text, Instant.now());
    }

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
}
