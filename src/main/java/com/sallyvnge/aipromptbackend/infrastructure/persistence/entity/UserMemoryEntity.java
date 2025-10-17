package com.sallyvnge.aipromptbackend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "user_memory")
@Getter
@Setter
public class UserMemoryEntity {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name="profile_json", columnDefinition = "jsonb", nullable = false)
    private String profileJson;

    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;
}
