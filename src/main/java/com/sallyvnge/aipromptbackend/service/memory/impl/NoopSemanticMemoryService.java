package com.sallyvnge.aipromptbackend.service.memory.impl;

import com.sallyvnge.aipromptbackend.service.memory.SemanticMemoryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.memory.semantic.enabled", havingValue = "false", matchIfMissing = true)
public class NoopSemanticMemoryService implements SemanticMemoryService {

    @Override
    public void indexChunk(UUID userId, UUID threadId, String source, String text) {

    }

    @Override
    public List<String> retrieve(UUID userId, String query, int k) {
        return List.of();
    }
}
