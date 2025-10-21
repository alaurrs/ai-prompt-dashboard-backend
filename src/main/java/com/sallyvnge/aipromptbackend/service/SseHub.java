package com.sallyvnge.aipromptbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@Slf4j
public class SseHub {

    private final Map<UUID, CopyOnWriteArraySet<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("ts", Instant.now().toString())));
        } catch (IOException e) {
            remove(userId, emitter);
        }

        return emitter;
    }

    public void emit(UUID userId, String eventName, Object payload) {
        Set<SseEmitter> set = emitters.get(userId);
        if (set == null || set.isEmpty()) return;
        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
    }

    public void keepalive() {
        Instant now = Instant.now();
        for (Map.Entry<UUID, CopyOnWriteArraySet<SseEmitter>> entry : emitters.entrySet()) {
            UUID userId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name("keepalive").data(Map.of("ts", now.toString())));
                } catch (IOException e) {
                    remove(userId, emitter);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 15000L)
    public void scheduledKeepalive() {
        keepalive();
    }

    private void remove(UUID userId, SseEmitter emitter) {
        CopyOnWriteArraySet<SseEmitter> set = emitters.get(userId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) emitters.remove(userId);
        }
        try { emitter.complete(); } catch (Exception ignored) {}
    }
}
