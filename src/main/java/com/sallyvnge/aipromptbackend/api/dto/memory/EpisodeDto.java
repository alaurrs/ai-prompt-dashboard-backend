package com.sallyvnge.aipromptbackend.api.dto.memory;

import java.time.Instant;
import java.util.UUID;

public record EpisodeDto(
        UUID id, String title, String detail, Instant occurredAt
) {
}
