package com.sallyvnge.aipromptbackend.api.dto.thread;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateThreadDto(
        @Size(max= 120) String title,
        @Size(max=64) String model,
        @Size(max=20000) String systemPrompt
) {
}
