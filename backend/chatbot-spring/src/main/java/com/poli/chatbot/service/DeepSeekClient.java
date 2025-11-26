package com.poli.chatbot.service;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekClient {

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api.key}")
    private String apiKey;

    public String complete(String prompt, String sessionId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(apiUrl);
            post.addHeader("Authorization", "Bearer " + apiKey);
            post.addHeader("Content-Type", "application/json");
            String body = "{\"model\": \"deepseek-chat\", \"messages\": [" +
                    "{\"role\": \"user\", \"content\": " + quote(prompt) + "}]" +
                    (sessionId != null ? ", \"session_id\": " + quote(sessionId) : "") + "}";
            post.setEntity(new StringEntity(body));
            ClassicHttpResponse resp = (ClassicHttpResponse) client.execute(post);
            String json = EntityUtils.toString(resp.getEntity());
            // naive extract text
            int i = json.indexOf("\"content\":");
            if (i >= 0) {
                int start = json.indexOf('"', i + 10) + 1;
                int end = json.indexOf('"', start);
                if (start > 0 && end > start) {
                    return json.substring(start, end);
                }
            }
            return json;
        } catch (Exception e) {
            return "[error] " + e.getMessage();
        }
    }

    private String quote(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }
}
