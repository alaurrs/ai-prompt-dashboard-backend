package com.sallyvnge.aipromptbackend.api.dto.thread;

import com.sallyvnge.aipromptbackend.domain.TitleSource;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ThreadDto(
        UUID id,
        String title,
        TitleSource titleSource,
        String model,
        String status,
        String systemPrompt,
        String summary,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
