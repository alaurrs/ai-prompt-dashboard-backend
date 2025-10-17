package com.sallyvnge.aipromptbackend.service.memory.impl;

import com.sallyvnge.aipromptbackend.config.AppAiModelProperties;
import com.sallyvnge.aipromptbackend.config.AppMemoryProperties;
import com.sallyvnge.aipromptbackend.domain.port.AiProvider;
import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.ThreadSummaryEntity;
import com.sallyvnge.aipromptbackend.infrastructure.persistence.repository.ThreadSummaryRepository;
import com.sallyvnge.aipromptbackend.service.memory.ThreadSummaryService;
import com.sallyvnge.aipromptbackend.service.memory.TokenEstimator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThreadSummaryServiceImpl implements ThreadSummaryService {

    private final ThreadSummaryRepository threadSummaryRepository;
    private final AppMemoryProperties props;
    private final AppAiModelProperties models;
    private final AiProvider ai;

    @Override
    @Transactional(readOnly = true)
    public String getSummary(UUID threadId) {
        return threadSummaryRepository.findById(threadId)
                .map(ThreadSummaryEntity::getSummaryText)
                .orElse("");
    }

    @Override
    @Transactional
    public void updateSummary(UUID threadId, String latestUserMsg, String latestAssistantMsg) {
        ThreadSummaryEntity threadSummary = threadSummaryRepository.findById(threadId).orElseGet(() -> {
                    ThreadSummaryEntity ts = new ThreadSummaryEntity();
                    ts.setThreadId(threadId);
                    return ts;
                }
        );

        String current = threadSummary.getSummaryText() != null ? threadSummary.getSummaryText() : "";
        String updated = (current + "\n[U] " + safe(latestUserMsg) + "\n[A] " + safe(latestAssistantMsg)).trim();

        try {
            String system = """
                    Tu es un assistant qui maintient un résumé concis, factuel et neutre d'une conversation.
                    - Garde les informations durables utiles pour la suite.
                    - Supprime le bruit, les répétitions.
                    - Maximum ~%d tokens (approx).
                    - Langue: conserve celle des extraits (ici: FR).
                    """.formatted(props.getSummary().getMaxTokens());
            String user = buildUpdatePrompt(current, latestUserMsg, latestAssistantMsg);

            updated = ai.completeOnce(models.getSummary(), system, user).trim();

            if (TokenEstimator.estimateTokens(updated) > props.getSummary().getMaxTokens()) {
                String compressUser = """
                        Résume de façon encore plus compacte (garde les faits clés, décisions, préférences, TODO explicites).
                        
                        Texte:
                        %s
                        """.formatted(updated);
                updated = ai.completeOnce(models.getSummary(), system, compressUser).trim();
            }
        } catch (Exception e) {
            updated = (current + "\n[U] " + safe(latestUserMsg) + "\n[A] " + safe(latestAssistantMsg)).trim();
        }

        threadSummary.setSummaryText(updated);
        threadSummary.setTokensEstimated(updated.length() / 4);
        threadSummary.setUpdatedAt(Instant.now());
        threadSummaryRepository.save(threadSummary);
    }

    private String buildUpdatePrompt(String currentSummary, String latestUser, String latestAssistant) {
        return """
                Résumé actuel:
                ---
                %s
                ---
                
                Nouveaux messages:
                [User] %s
                [Assistant] %s
                
                Mets à jour le résumé en intégrant uniquement les nouveautés et en corrigeant si besoin.
                Forme: paragraphe(s) courts, listes si utile.
                """.formatted(
                currentSummary == null ? "" : currentSummary,
                safe(latestUser),
                safe(latestAssistant)
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
