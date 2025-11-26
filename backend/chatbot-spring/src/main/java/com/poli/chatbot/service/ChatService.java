package com.poli.chatbot.service;

import com.poli.chatbot.model.ChatMessage;
import com.poli.chatbot.model.ChatSession;
import com.poli.chatbot.repo.ChatMessageRepository;
import com.poli.chatbot.repo.ChatSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;
    private final DeepSeekClient deepSeekClient;
    private final RosApiClient rosClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AnalyticsClient analyticsClient;

    public ChatService(ChatMessageRepository messageRepository,
                       ChatSessionRepository sessionRepository,
                       DeepSeekClient deepSeekClient,
                       RosApiClient rosClient,
                       AnalyticsClient analyticsClient) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.deepSeekClient = deepSeekClient;
        this.rosClient = rosClient;
        this.analyticsClient = analyticsClient;
    }

    public ChatSession getOrCreateSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseGet(() -> sessionRepository.save(new ChatSession(sessionId, Instant.now(), "active")));
    }

    public ChatMessage handleUserMessage(String sessionId, String question) {
        ChatSession session = getOrCreateSession(sessionId);
        // Save user question
        ChatMessage userMsg = ChatMessage.user(sessionId, question);
        messageRepository.save(userMsg);
        String t1 = question.toLowerCase();
        boolean maybeAction = (t1.contains("prende") || t1.contains("enciende") || t1.contains("apaga") || t1.contains("apagar")
            || t1.contains("hab") || t1.contains("coc") || t1.contains("sal") || t1.contains("todo") || t1.contains("todas"));

        String answer;
        boolean executedByAI = false;
        if (maybeAction) {
            String system = "Eres un asistente de hogar inteligente. \n" +
                    "Responde SIEMPRE en JSON estricto con este esquema: \n" +
                    "{\\\"reply\\\": \\\"texto\\\", \\\"actions\\\": [{\\\"room\\\": \\\"hab|coc|sal|all\\\", \\\"state\\\": \\\"on|off\\\"}]}\n" +
                    "- Usa room=hab para habitación, coc para cocina, sal para sala, o all para todas.\n" +
                    "- Interpreta frases como 'prende todo', 'apaga sala y cocina', etc.\n" +
                    "- reply debe ser amigable y corto.\n" +
                    "- actions refleja exactamente lo que hay que ejecutar.";
            String raw = deepSeekClient.completeStructured(system, question, sessionId);
            answer = raw;
            try {
                JsonNode root = mapper.readTree(raw);
                if (root.has("reply")) {
                    answer = root.path("reply").asText();
                }
                if (root.has("actions") && root.path("actions").isArray()) {
                    StringBuilder executed = new StringBuilder();
                    for (JsonNode a : root.path("actions")) {
                        String room = a.path("room").asText("");
                        String state = a.path("state").asText("");
                        if (room.isBlank() || state.isBlank()) continue;
                        if (room.equals("all")) {
                            boolean r1 = rosClient.toggle("hab", state);
                            boolean r2 = rosClient.toggle("coc", state);
                            boolean r3 = rosClient.toggle("sal", state);
                            analyticsClient.recordState("hab", state);
                            analyticsClient.recordState("coc", state);
                            analyticsClient.recordState("sal", state);
                            executed.append(String.format("[hogar] %s todo: %s/%s/%s\n", state, r1?"OK":"FAIL", r2?"OK":"FAIL", r3?"OK":"FAIL"));
                        } else {
                            boolean ok = rosClient.toggle(room, state);
                            analyticsClient.recordState(room, state);
                            executed.append(String.format("[hogar] %s %s: %s\n", state, room, ok?"OK":"FAIL"));
                        }
                    }
                    executedByAI = true;
                    if (executed.length() > 0) {
                        answer = executed.toString().trim() + "\n\n" + answer;
                    }
                }
            } catch (Exception ignored) {
            }
        } else {
            // Pregunta general → respuesta normal del modelo
            answer = deepSeekClient.complete(question, sessionId);
            if (answer.startsWith("[error]")) {
                answer = "Lo siento, no pude responder en este momento.";
            }
        }

        // Fallback simple si la IA falló (error 4xx/5xx) o no devolvió JSON válido
        if (maybeAction && !executedByAI) {
            String fallbackNote = null;
            String t = t1;
            boolean on = (t1.contains("prende") || t1.contains("enciende") || t1.contains("encender"));
            boolean off = (t1.contains("apaga") || t1.contains("apagar"));
            if (t1.contains("todo") || t1.contains("todas")) {
                String state = on ? "on" : (off ? "off" : "");
                if (!state.isEmpty()) {
                    boolean r1 = rosClient.toggle("hab", state);
                    boolean r2 = rosClient.toggle("coc", state);
                    boolean r3 = rosClient.toggle("sal", state);
                    analyticsClient.recordState("hab", state);
                    analyticsClient.recordState("coc", state);
                    analyticsClient.recordState("sal", state);
                    fallbackNote = String.format("[hogar] %s todo: %s/%s/%s", state.equals("on")?"Encendiendo":"Apagando", r1?"OK":"FAIL", r2?"OK":"FAIL", r3?"OK":"FAIL");
                }
            } else {
                String room = null;
                if (t1.contains("hab")) room = "hab";
                else if (t1.contains("coc")) room = "coc";
                else if (t1.contains("sal")) room = "sal";
                String state = on ? "on" : (off ? "off" : "");
                if (room != null && !state.isEmpty()) {
                    boolean ok = rosClient.toggle(room, state);
                    analyticsClient.recordState(room, state);
                    fallbackNote = String.format("[hogar] %s %s: %s", state.equals("on")?"Encendiendo":"Apagando", room, ok?"OK":"FAIL");
                }
            }
            if (fallbackNote != null) {
                if (answer.startsWith("[error]")) {
                    answer = fallbackNote;
                } else {
                    answer = fallbackNote + "\n\n" + answer;
                }
            }
        }
        // Save assistant reply
        ChatMessage assistantMsg = ChatMessage.assistant(sessionId, answer);
        messageRepository.save(assistantMsg);
        // Update session state if needed
        session.setLastUpdated(Instant.now());
        sessionRepository.save(session);
        return assistantMsg;
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return messageRepository.findBySessionIdOrderByTsAsc(sessionId);
    }

}
