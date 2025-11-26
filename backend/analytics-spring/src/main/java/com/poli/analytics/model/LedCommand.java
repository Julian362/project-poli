package com.poli.analytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("led_commands")
public class LedCommand {
    @Id
    private String id;
    private String command; // H,h,C,c,S,s,X
    private String source;  // ros-cli-api, other-backend, etc.
    private Instant ts;

    public LedCommand() {}

    public LedCommand(String command, String source, Instant ts) {
        this.command = command;
        this.source = source;
        this.ts = ts;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
}
