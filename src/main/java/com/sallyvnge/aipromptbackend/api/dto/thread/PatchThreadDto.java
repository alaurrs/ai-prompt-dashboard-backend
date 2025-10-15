package com.sallyvnge.aipromptbackend.api.dto.thread;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatchThreadDto(
        @Size(max=120) String title,
        @Pattern(regexp = "active|archived|deleted") String status,
        @Size(max=64) String model,
        @Size(max=20000) String systemPrompt,
        @NotNull Long version
) {
}
