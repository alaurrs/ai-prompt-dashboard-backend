package com.sallyvnge.aipromptbackend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "thread_summary")
@Getter
@Setter
public class ThreadSummaryEntity {

    @Id
    @Column(name="thread_id", nullable = false, updatable = false)
    private UUID threadId;

    @Column(name="summary_text", nullable = false, columnDefinition = "text")
    private String summaryText;

    @Column(name="tokens_estimated", nullable = false)
    private int tokensEstimated;

    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;
}
