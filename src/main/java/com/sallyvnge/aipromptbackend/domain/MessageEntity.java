package com.sallyvnge.aipromptbackend.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="messages")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class MessageEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID) private UUID id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="thread_id", nullable=false)
    private ThreadEntity thread;

    @Column(nullable=false) private String author;
    @Column(nullable=false) private Integer position;
    @Column(nullable=false) private String status = "complete";
    @Column(columnDefinition="text") private String content;
    @Column(columnDefinition="text") private String contentDelta;

    private String model;
    private Integer usagePromptTokens;
    private Integer usageCompletionTokens;
    private Integer latencyMs;

    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}