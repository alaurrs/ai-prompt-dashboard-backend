package com.sallyvnge.aipromptbackend.config;

import java.util.UUID;

public record CurrentUser(
        UUID id,
        String email,
        String displayName
) {
}
