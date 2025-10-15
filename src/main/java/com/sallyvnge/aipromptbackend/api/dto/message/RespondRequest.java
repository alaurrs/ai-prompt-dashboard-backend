package com.sallyvnge.aipromptbackend.api.dto.message;


public record RespondRequest(
        String prompt,
        String model,
        String systemPrompt
) {
}
