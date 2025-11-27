package com.poli.chatbot.service;

import com.poli.chatbot.model.ChatMessage;
import com.poli.chatbot.model.ActionEvent;
import com.poli.chatbot.model.ChatSession;
import com.poli.chatbot.repo.ChatMessageRepository;
import com.poli.chatbot.repo.ActionEventRepository;
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
    private final ActionEventRepository actionEventRepository;

    public ChatService(ChatMessageRepository messageRepository,
                       ChatSessionRepository sessionRepository,
                       DeepSeekClient deepSeekClient,
                       RosApiClient rosClient,
                       AnalyticsClient analyticsClient,
                       ActionEventRepository actionEventRepository) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.deepSeekClient = deepSeekClient;
        this.rosClient = rosClient;
        this.analyticsClient = analyticsClient;
        this.actionEventRepository = actionEventRepository;
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
        String eventSummary = null; // concise summary
        String eventSource = null; // ai|fallback
        String rawActionsJson = null;
        java.util.List<String> affectedRooms = new java.util.ArrayList<>();
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
                    rawActionsJson = root.path("actions").toString();
                    boolean habOn=false,cocOn=false,salOn=false,habOff=false,cocOff=false,salOff=false;
                    for (JsonNode a : root.path("actions")) {
                        String room = a.path("room").asText("");
                        String state = a.path("state").asText("");
                        if (room.isBlank() || state.isBlank()) continue;
                        if (room.equals("all")) {
                            boolean rHab = rosClient.toggle("hab", state);
                            boolean rCoc = rosClient.toggle("coc", state);
                            boolean rSal = rosClient.toggle("sal", state);
                            analyticsClient.recordState("hab", state);
                            analyticsClient.recordState("coc", state);
                            analyticsClient.recordState("sal", state);
                            if (state.equals("on")) { habOn=true;cocOn=true;salOn=true; } else { habOff=true;cocOff=true;salOff=true; }
                            affectedRooms.add("hab"); affectedRooms.add("coc"); affectedRooms.add("sal");
                        } else {
                            boolean ok = rosClient.toggle(room, state);
                            analyticsClient.recordState(room, state);
                            if (room.equals("hab")) { if (state.equals("on")) habOn=true; else habOff=true; }
                            if (room.equals("coc")) { if (state.equals("on")) cocOn=true; else cocOff=true; }
                            if (room.equals("sal")) { if (state.equals("on")) salOn=true; else salOff=true; }
                            affectedRooms.add(room);
                        }
                    }
                    executedByAI = true;
                    String concise = buildSummaryLine(habOn,cocOn,salOn,habOff,cocOff,salOff);
                    eventSummary = concise;
                    eventSource = "ai";
                    if (!concise.isBlank()) {
                        answer = concise + (answer.isBlank() ? "" : " " + answer);
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
            boolean habOn=false,cocOn=false,salOn=false,habOff=false,cocOff=false,salOff=false;
            if (t1.contains("todo") || t1.contains("todas")) {
                String state = on ? "on" : (off ? "off" : "");
                if (!state.isEmpty()) {
                    rosClient.toggle("hab", state); rosClient.toggle("coc", state); rosClient.toggle("sal", state);
                    analyticsClient.recordState("hab", state); analyticsClient.recordState("coc", state); analyticsClient.recordState("sal", state);
                    if (state.equals("on")) { habOn=true;cocOn=true;salOn=true; } else { habOff=true;cocOff=true;salOff=true; }
                }
            } else {
                String room = null;
                if (t1.contains("hab")) room = "hab"; else if (t1.contains("coc")) room = "coc"; else if (t1.contains("sal")) room = "sal";
                String state = on ? "on" : (off ? "off" : "");
                if (room != null && !state.isEmpty()) {
                    rosClient.toggle(room, state);
                    analyticsClient.recordState(room, state);
                    if (room.equals("hab")) { if (state.equals("on")) habOn=true; else habOff=true; }
                    if (room.equals("coc")) { if (state.equals("on")) cocOn=true; else cocOff=true; }
                    if (room.equals("sal")) { if (state.equals("on")) salOn=true; else salOff=true; }
                }
            }
            fallbackNote = buildSummaryLine(habOn,cocOn,salOn,habOff,cocOff,salOff);
            eventSummary = fallbackNote;
            eventSource = "fallback";
            if (habOn||habOff) affectedRooms.add("hab");
            if (cocOn||cocOff) affectedRooms.add("coc");
            if (salOn||salOff) affectedRooms.add("sal");
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
        // Persist ActionEvent if we executed something
        if (eventSummary != null && !eventSummary.isBlank() && !affectedRooms.isEmpty()) {
            // Determine actionType
            String actionType;
            if (eventSummary.contains("encendida") || eventSummary.contains("encendidas")) {
                if (eventSummary.contains("apagada") || eventSummary.contains("apagadas")) actionType = "mixed"; else actionType = "on";
            } else if (eventSummary.contains("apagada") || eventSummary.contains("apagadas")) {
                actionType = "off";
            } else {
                actionType = "unknown";
            }
            ActionEvent ev = new ActionEvent(sessionId, affectedRooms, actionType, eventSummary, Instant.now(), eventSource == null ? "unknown" : eventSource, rawActionsJson);
            actionEventRepository.save(ev);
        }
        // Update session state if needed
        session.setLastUpdated(Instant.now());
        sessionRepository.save(session);
        return assistantMsg;
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return messageRepository.findBySessionIdOrderByTsAsc(sessionId);
    }
    // --- Helper UX formatting methods ---
    private String buildSummaryLine(boolean habOn, boolean cocOn, boolean salOn, boolean habOff, boolean cocOff, boolean salOff) {
        String onList = joinRooms(habOn, cocOn, salOn);
        String offList = joinRooms(habOff, cocOff, salOff);
        if (!onList.isEmpty() && offList.isEmpty()) {
            return onList + (isPlural(onList)?" encendidas." : " encendida.");
        }
        if (!offList.isEmpty() && onList.isEmpty()) {
            return offList + (isPlural(offList)?" apagadas." : " apagada.");
        }
        if (!onList.isEmpty() && !offList.isEmpty()) {
            return onList + (isPlural(onList)?" encendidas; " : " encendida; ") + offList + (isPlural(offList)?" apagadas." : " apagada.");
        }
        return "";
    }

    private String joinRooms(boolean hab, boolean coc, boolean sal) {
        StringBuilder sb = new StringBuilder();
        if (hab) sb.append(sb.length()>0?", habitación":"habitación");
        if (coc) sb.append(sb.length()>0?", cocina":"cocina");
        if (sal) sb.append(sb.length()>0?", sala":"sala");
        String out = sb.toString();
        if (out.contains(",")) {
            int lastComma = out.lastIndexOf(", ");
            if (lastComma >= 0) {
                out = out.substring(0, lastComma) + " y" + out.substring(lastComma + 1);
            }
        }
        return out;
    }

    private boolean isPlural(String list) {
        return list.contains(" y ");
    }
}
