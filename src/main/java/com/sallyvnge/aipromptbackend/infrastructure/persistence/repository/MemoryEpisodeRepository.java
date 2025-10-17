package com.sallyvnge.aipromptbackend.infrastructure.persistence.repository;

import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.MemoryEpisodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryEpisodeRepository extends JpaRepository<MemoryEpisodeEntity, UUID> {
    List<MemoryEpisodeEntity> findTop20ByUserIdOrderByOccurredAtDesc(UUID userId);
    List<MemoryEpisodeEntity> findTop20ByThreadIdOrderByOccurredAtDesc(UUID threadId);
}
