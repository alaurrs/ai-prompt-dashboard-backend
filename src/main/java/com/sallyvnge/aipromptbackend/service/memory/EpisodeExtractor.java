package com.sallyvnge.aipromptbackend.service.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EpisodeExtractor {

    private final EpisodeService episodeService;

    private static final Pattern DECISION = Pattern.compile("(?i)\\b(j'ai décidé|je décide|je vais|dorénavant|désormais)\\b");
    private static final Pattern PREFERENCE = Pattern.compile("(?i)\\b(préfère|préférence|j'aime|j'apprécie)\\b");
    private static final Pattern DEADLINE = Pattern.compile("(?i)\\b(d'ici|avant le|date limite|deadline|au plus tard)\\b");

    public void maybeCreateEpisodes(UUID userId, UUID threadId, String text) {
        if (text == null || text.isBlank()) return;
        String t = text.strip();

        if (DECISION.matcher(t).find()) {
            episodeService.create(userId, threadId, "Décision", truncate(t, 600));
        } else if (PREFERENCE.matcher(t).find()) {
            episodeService.create(userId, threadId, "Préférence", truncate(t, 600));
        } else if (DEADLINE.matcher(t).find()) {
            episodeService.create(userId, threadId, "Échéance", truncate(t, 600));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
