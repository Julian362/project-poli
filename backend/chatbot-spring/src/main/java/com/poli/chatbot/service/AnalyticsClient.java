package com.poli.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AnalyticsClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    public AnalyticsClient(@Value("${analytics.api.baseUrl:http://analytics-spring:8082}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean recordState(String room, String state) {
        String cmd = map(room, state);
        if (cmd == null) return false;
        try {
            String url = baseUrl + "/api/analytics/commands/record?command=" + cmd + "&source=chatbot";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() >= 200 && res.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private String map(String room, String state) {
        boolean on = "on".equalsIgnoreCase(state);
        boolean off = "off".equalsIgnoreCase(state);
        if (!(on || off)) return null;
        switch (room) {
            case "hab": return on ? "H" : "h";
            case "coc": return on ? "C" : "c";
            case "sal": return on ? "S" : "s";
            default: return null;
        }
    }
}
