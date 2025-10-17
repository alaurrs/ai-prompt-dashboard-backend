package com.sallyvnge.aipromptbackend.service.memory;

import java.util.List;
import java.util.UUID;

public interface SemanticMemoryService {
    void indexChunk(UUID userId, UUID threadId, String source, String text);
    List<String> retrieve(UUID userId, String query, int k);
}
