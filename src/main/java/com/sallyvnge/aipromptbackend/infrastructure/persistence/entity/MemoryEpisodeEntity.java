package com.sallyvnge.aipromptbackend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_episode")
@Getter
@Setter
public class MemoryEpisodeEntity {
    @Id
    private UUID id;

    @Column(name="user_id", nullable = false)
    private UUID userId;

    private UUID threadId;

    @Column(name="occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "detail", nullable = false, columnDefinition = "text")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
