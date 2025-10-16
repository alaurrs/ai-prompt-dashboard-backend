package com.sallyvnge.aipromptbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final long expMinutes;
    private final long refreshDays;


    public JwtServiceImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.exp-minutes:15}") long expMinutes,
            @Value("${jwt.refresh-days:14}") long refreshDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expMinutes = expMinutes;
        this.refreshDays = refreshDays;
    }

    @Override
    public String issue(UUID userId, String email, String displayName, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("displayName", displayName)
                .claim("roles", roles == null ? List.of("ROLE_USER") : roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String issueRefresh(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("typ", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshDays, ChronoUnit.DAYS)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public JwtPrincipal validateAndParse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();

            Date exp = claims.getExpiration();
            if (exp != null && exp.before(new Date())) {
                throw new JwtException("Token expired");
            }

            String sub = claims.getSubject();
            String email = claims.get("email", String.class);
            String displayName = claims.get("displayName", String.class);
            if (sub == null || sub.isBlank()) {
                throw new JwtException("Missing subject");
            }
            UUID userId = UUID.fromString(sub);

            List<String> roles = extractRoles(claims);

            String tokenType = Objects.toString(claims.get("typ"), "access");

            return new JwtPrincipal(userId, email, displayName, roles, tokenType);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid or expired JWT: " + e.getMessage(), e);
        }

    }

    @Override
    public boolean isRefreshToken(Claims claims) {
        return "refresh".equalsIgnoreCase(Objects.toString(claims.get("typ"), ""));
    }

    private static List<String> extractRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(Objects::toString).toList();
        }
        if (raw instanceof String str && !str.isBlank()) {
            String s = str.trim();
            if (s.startsWith("[") && s.endsWith("]")) {
                s = s.substring(1, s.length() - 1).trim();
            }
            if (s.isEmpty()) return List.of("ROLE_USER");
            return List.of(s.split("\\s*,\\s*"));
        }
        return List.of("ROLE_USER");
    }
}
