package com.sallyvnge.aipromptbackend.api.dto.auth;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
