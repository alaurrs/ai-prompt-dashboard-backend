package com.sallyvnge.aipromptbackend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_chunk")
@Getter
@Setter
public class MemoryChunkEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private UUID threadId;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
