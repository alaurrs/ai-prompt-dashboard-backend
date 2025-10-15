package com.sallyvnge.aipromptbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threads")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ThreadEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private String title;
    private String model;

    @Column(nullable = false)
    private String status = "active";
    @Column(columnDefinition = "text")
    private String systemPrompt;
    @Column(columnDefinition = "text")
    private String summary;

    @Version
    private long version;

    @Column(name="created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name="updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }


    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
