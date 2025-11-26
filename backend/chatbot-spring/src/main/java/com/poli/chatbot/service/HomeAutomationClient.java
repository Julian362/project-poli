package com.poli.chatbot.service;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HomeAutomationClient {

    @Value("${ros.api.base:http://ros-cli-api:8000}")
    private String rosBase;

    public Result trigger(String room, String action) {
        String path = buildPath(room, action);
        if (path == null) {
            return Result.error("unknown_room_or_action");
        }
        String url = rosBase + path;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            ClassicHttpResponse resp = (ClassicHttpResponse) client.execute(get);
            int code = resp.getCode();
            String body = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
            if (code >= 200 && code < 300) {
                return Result.ok(body);
            }
            return Result.error("HTTP " + code + ": " + body);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private String buildPath(String room, String action) {
        String r = normalize(room);
        String a = normalize(action);
        String onoff = ("on".equals(a) ? "/on" : ("off".equals(a) ? "/off" : null));
        if (onoff == null) return null;
        if (r.equals("hab") || r.equals("habitacion") || r.equals("habitación")) return "/hab" + onoff;
        if (r.equals("coc") || r.equals("cocina")) return "/coc" + onoff;
        if (r.equals("sal") || r.equals("sala")) return "/sal" + onoff;
        return null;
    }

    private String normalize(String s) {
        return s.toLowerCase().trim();
    }

    public static class Result {
        public final boolean ok;
        public final String detail;
        private Result(boolean ok, String detail) { this.ok = ok; this.detail = detail; }
        public static Result ok(String d) { return new Result(true, d); }
        public static Result error(String d) { return new Result(false, d); }
    }
}