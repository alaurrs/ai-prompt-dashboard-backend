package com.sallyvnge.aipromptbackend.service.memory.impl;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.EmbeddingModel;
import com.sallyvnge.aipromptbackend.config.AppAiModelProperties;
import com.sallyvnge.aipromptbackend.infrastructure.persistence.repository.MemoryChunkRepository;
import com.sallyvnge.aipromptbackend.service.memory.SemanticMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.memory.semantic.enabled", havingValue = "true")
@Slf4j
public class PgvectorSemanticMemoryService implements SemanticMemoryService {

    private final OpenAIClient openAIClient;
    private final AppAiModelProperties models;
    private final JdbcTemplate jdbc;
    @SuppressWarnings("unused")
    private final MemoryChunkRepository memoryChunkRepository;

    @Override
    public void indexChunk(UUID userId, UUID threadId, String source, String text) {
        if (text == null || text.isBlank()) return;
        try {
            double[] embedding = embed(text);
            String embeddingExpr = toVectorExpr(embedding);

            String sql = "INSERT INTO memory_chunk (id, user_id, thread_id, source, content, embedding, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, " + embeddingExpr + ", now())";

            jdbc.update(sql, UUID.randomUUID(), userId, threadId, source, text);
        } catch (Exception e) {
            log.warn("indexChunk failed: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<String> retrieve(UUID userId, String query, int k) {
        if (query == null || query.isBlank()) return List.of();
        int limit = Math.max(1, Math.min(k, 20));
        try {
            double[] embedding = embed(query);
            String embeddingExpr = toVectorExpr(embedding);

            String sql = "SELECT content FROM memory_chunk WHERE user_id = ? ORDER BY embedding <=> " + embeddingExpr + " LIMIT " + limit;
            return jdbc.query(sql, (rs, rowNum) -> rs.getString(1), userId);
        } catch (Exception e) {
            log.warn("retrieve failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private double[] embed(String text) {
        var params = EmbeddingCreateParams.builder()
                .model(EmbeddingModel.of(models.getEmbedding()))
                .input(text)
                .build();
        var res = openAIClient.embeddings().create(params);
        var vec = res.data().get(0).embedding();
        double[] out = new double[vec.size()];
        for (int i = 0; i < vec.size(); i++) out[i] = ((Number) vec.get(i)).doubleValue();
        return out;
    }

    private static String toVectorExpr(double[] v) {
        String body = Arrays.stream(v)
                .mapToObj(d -> String.format(Locale.ROOT, "%.8f", d))
                .collect(Collectors.joining(","));
        return "ARRAY[" + body + "]::vector";
    }
}
