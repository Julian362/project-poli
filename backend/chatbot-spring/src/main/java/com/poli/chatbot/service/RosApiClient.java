package com.poli.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class RosApiClient {

    @Value("${ros.api.baseUrl:http://ros-cli-api:8000}")
    private String baseUrl;

    private final HttpClient client = HttpClient.newHttpClient();

    public boolean toggle(String room, String state) {
        try {
            String url = baseUrl + "/" + room + "/" + state;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
