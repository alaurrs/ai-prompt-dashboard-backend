package com.sallyvnge.aipromptbackend.infrastructure.persistence.repository;

import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.MemoryChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryChunkRepository extends JpaRepository<MemoryChunkEntity, UUID> {
    List<MemoryChunkEntity> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<MemoryChunkEntity> findTop100ByThreadIdOrderByCreatedAtDesc(UUID threadId);
}
