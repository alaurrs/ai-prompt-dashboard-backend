package com.sallyvnge.aipromptbackend.api.dto.thread;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateThreadWithMessageDto(
        @NotBlank
        @Size(max=24000)
        String content,
        String systemPrompt,
        String model
) {

}
