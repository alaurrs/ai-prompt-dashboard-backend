package com.sallyvnge.aipromptbackend.security;

import io.jsonwebtoken.Claims;

import java.util.List;
import java.util.UUID;

public interface JwtService {
    String issue(UUID userId, String email, String displayName, List<String> roles);
    String issueRefresh(UUID userId);

    JwtPrincipal validateAndParse(String token);
    boolean isRefreshToken(Claims claims);


    record JwtPrincipal(UUID userId, String email, String displayName, List<String> roles, String tokenType) {}
}
