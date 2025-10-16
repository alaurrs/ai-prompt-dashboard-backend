package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.api.dto.auth.LoginRequest;
import com.sallyvnge.aipromptbackend.domain.UserEntity;
import com.sallyvnge.aipromptbackend.repository.UserRepository;
import com.sallyvnge.aipromptbackend.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            User user = (User) auth.getPrincipal();
            UserEntity userEntity = userRepository.findByEmail(user.getUsername()).orElseThrow();

            UUID userId = userEntity.getId();
            String email = user.getUsername();
            String displayName = userEntity.getDisplayName();

            List<String> roles = user.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority).toList();

            String access = jwtService.issue(userId, email, displayName, roles);
            String refresh = jwtService.issueRefresh(userId);
            return ResponseEntity.ok(Map.of(
                    "accessToken", access,
                    "refreshToken", refresh,
                    "tokenType", "Bearer",
                    "expiresIn", Duration.ofMinutes(15).toSeconds()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.getOrDefault("refreshToken", "");
        if (refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid refresh token"));
        }

        try {
            JwtService.JwtPrincipal principal = jwtService.validateAndParse(refreshToken);

            if (!"refresh".equalsIgnoreCase(principal.tokenType())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token type"));
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(principal.email());

            List<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority).toList();

            UUID userId = principal.userId();
            String newAccess = jwtService.issue(userId, principal.email(), principal.displayName(), roles);
            String newRefresh = jwtService.issueRefresh(userId);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccess,
                    "refreshToken", newRefresh,
                    "tokenType", "Bearer",
                    "expiresIn", Duration.ofMinutes(15).toSeconds()
            ));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }
    }
}
