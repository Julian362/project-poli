package com.poli.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekClient {

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;
    @Value("${openrouter.api.key:}")
    private String apiKey;
    @Value("${openrouter.model:deepseek/deepseek-r1-0528:free}")
    private String model;
    @Value("${openrouter.maxRetries:3}")
    private int maxRetries;
    @Value("${openrouter.retryDelayMillis:1500}")
    private long retryDelayMillis;

    private final ObjectMapper mapper = new ObjectMapper();

    public String complete(String prompt, String sessionId) {
        if (apiKey == null || apiKey.isBlank()) {
            return "[error] openrouter_api_key_missing";
        }
        String body = "{" +
            "\"model\": " + quote(model) + "," +
            "\"messages\": [{\"role\": \"user\", \"content\": " + quote(prompt) + "}]" +
            "}";
        int attempt = 0;
        String lastError = null;
        while (attempt < maxRetries) {
            attempt++;
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(apiUrl);
                post.addHeader("Authorization", "Bearer " + apiKey);
                post.addHeader("HTTP-Referer", "https://openrouter.ai");
                post.addHeader("X-Title", "Project Poli Chatbot");
                post.addHeader("Content-Type", "application/json");
                post.setEntity(new StringEntity(body));
                ClassicHttpResponse resp = (ClassicHttpResponse) client.execute(post);
                int status = resp.getCode();
                String json = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
                if (status == 429 || status == 503) {
                    lastError = json;
                    try { Thread.sleep(retryDelayMillis * attempt); } catch (InterruptedException ignored) {}
                    continue;
                }
                if (status >= 400) {
                    return "[error] HTTP " + status + ": " + json;
                }
                return extractContent(json);
            } catch (Exception e) {
                lastError = e.getMessage();
                try { Thread.sleep(retryDelayMillis); } catch (InterruptedException ignored) {}
            }
        }
        return "[error] rate_limit_exhausted attempts=" + maxRetries + " last=" + (lastError == null ? "unknown" : lastError);
    }

    public String completeStructured(String systemPrompt, String userPrompt, String sessionId) {
        if (apiKey == null || apiKey.isBlank()) {
            return "[error] openrouter_api_key_missing";
        }
        String body = "{" +
            "\"model\": " + quote(model) + "," +
            "\"messages\": [" +
            "{\"role\": \"system\", \"content\": " + quote(systemPrompt) + "}," +
            "{\"role\": \"user\", \"content\": " + quote(userPrompt) + "}" +
            "]" +
            "}";
        int attempt = 0;
        String lastError = null;
        while (attempt < maxRetries) {
            attempt++;
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(apiUrl);
                post.addHeader("Authorization", "Bearer " + apiKey);
                post.addHeader("HTTP-Referer", "https://openrouter.ai");
                post.addHeader("X-Title", "Project Poli Chatbot");
                post.addHeader("Content-Type", "application/json");
                post.setEntity(new StringEntity(body));
                ClassicHttpResponse resp = (ClassicHttpResponse) client.execute(post);
                int status = resp.getCode();
                String json = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
                if (status == 429 || status == 503) {
                    lastError = json;
                    try { Thread.sleep(retryDelayMillis * attempt); } catch (InterruptedException ignored) {}
                    continue;
                }
                if (status >= 400) {
                    return "[error] HTTP " + status + ": " + json;
                }
                return extractContent(json);
            } catch (Exception e) {
                lastError = e.getMessage();
                try { Thread.sleep(retryDelayMillis); } catch (InterruptedException ignored) {}
            }
        }
        return "[error] rate_limit_exhausted attempts=" + maxRetries + " last=" + (lastError == null ? "unknown" : lastError);
    }

    private String extractContent(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode contentNode = choices.get(0).path("message").path("content");
                if (!contentNode.isMissingNode()) {
                    return contentNode.asText();
                }
            }
            return json;
        } catch (Exception e) {
            return json;
        }
    }

    private String quote(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }
}
