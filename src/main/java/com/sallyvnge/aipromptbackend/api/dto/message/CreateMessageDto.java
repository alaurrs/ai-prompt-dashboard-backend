package com.sallyvnge.aipromptbackend.api.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageDto(
        @NotBlank String author,
        @NotBlank @Size(max=24000) String content,
        String clientMessageId
) {
}
