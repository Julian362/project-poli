package com.poli.analytics.controller;

import com.poli.analytics.model.LedCommand;
import com.poli.analytics.repo.LedCommandRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final LedCommandRepository repo;

    public AnalyticsController(LedCommandRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/commands/counts")
    public Map<String, Long> counts() {
        List<LedCommand> latest = repo.findTop100ByOrderByTsDesc();
        Map<String, Long> acc = new LinkedHashMap<>();
        for (String k : List.of("H","h","C","c","S","s","X")) acc.put(k, 0L);
        latest.forEach(c -> acc.compute(c.getCommand(), (k,v) -> v == null ? 1 : v + 1));
        return acc;
    }

    @GetMapping("/commands/top")
    public Map<String, Object> top(@RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                   Instant since) {
        List<LedCommand> src = since != null ? repo.findByTsAfterOrderByTsDesc(since)
                                             : repo.findTop100ByOrderByTsDesc();
        Map<String, Long> acc = new HashMap<>();
        src.forEach(c -> acc.merge(c.getCommand(), 1L, Long::sum));
        String most = acc.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("none");
        return Map.of("top", most, "counts", acc, "sample", src.size());
    }

    @GetMapping("/lights/state")
    public Map<String, String> lightsState() {
        // Compute from latest commands
        List<LedCommand> latest = repo.findTop100ByOrderByTsDesc();
        String h = "unknown", c = "unknown", s = "unknown";
        for (LedCommand cmd : latest) {
            switch (cmd.getCommand()) {
                case "H" -> { if (h.equals("unknown")) h = "on"; }
                case "h" -> { if (h.equals("unknown")) h = "off"; }
                case "C" -> { if (c.equals("unknown")) c = "on"; }
                case "c" -> { if (c.equals("unknown")) c = "off"; }
                case "S" -> { if (s.equals("unknown")) s = "on"; }
                case "s" -> { if (s.equals("unknown")) s = "off"; }
            }
            if (!h.equals("unknown") && !c.equals("unknown") && !s.equals("unknown")) break;
        }
        return Map.of("habitacion", h, "cocina", c, "sala", s);
    }

    @PostMapping("/commands/record")
    public ResponseEntity<Map<String, Object>> record(@RequestParam String command,
                                                      @RequestParam(required = false) String source) {
        LedCommand c = new LedCommand(command, source == null ? "unknown" : source, Instant.now());
        repo.save(c);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
