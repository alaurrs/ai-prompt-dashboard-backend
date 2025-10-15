package com.sallyvnge.aipromptbackend.api.dto.message;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MessageDto(
        UUID id, UUID threadId, String author, Integer position, String status, String content, String contentDelta,
        String model, Integer usagePromptTokens, Integer usageCompletionTokens, Integer latencyMs,
        Instant createdAt
) {
}
