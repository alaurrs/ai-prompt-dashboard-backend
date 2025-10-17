package com.sallyvnge.aipromptbackend.service.memory;

import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.MemoryEpisodeEntity;

import java.util.List;
import java.util.UUID;

public interface EpisodeService {

    MemoryEpisodeEntity create(UUID userId, UUID threadId, String title, String detail);
    List<MemoryEpisodeEntity> latestForUser(UUID userId, int limit);
    List<MemoryEpisodeEntity> latestForThread(UUID threadId, int limit);
    void delete(UUID episodeId, UUID userId);

}
