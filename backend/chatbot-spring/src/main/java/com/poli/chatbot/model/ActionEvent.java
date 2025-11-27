package com.poli.chatbot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("action_events")
public class ActionEvent {
    @Id
    private String id;
    private String sessionId;
    private List<String> rooms; // hab|coc|sal or all expanded
    private String actionType; // on|off|mixed
    private String summary; // human friendly line
    private Instant ts;
    private String source; // ai|fallback
    private String raw; // raw JSON actions or parsed info

    public ActionEvent() {}

    public ActionEvent(String sessionId, List<String> rooms, String actionType, String summary, Instant ts, String source, String raw) {
        this.sessionId = sessionId;
        this.rooms = rooms;
        this.actionType = actionType;
        this.summary = summary;
        this.ts = ts;
        this.source = source;
        this.raw = raw;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public List<String> getRooms() { return rooms; }
    public void setRooms(List<String> rooms) { this.rooms = rooms; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }
}
