package com.sallyvnge.aipromptbackend.api.dto.thread;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ThreadDto(
        UUID id,
        String title,
        String model,
        String status,
        String systemPrompt,
        String summary,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
