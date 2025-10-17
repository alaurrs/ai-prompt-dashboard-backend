package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthPingController {

    @GetMapping("/ping")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> ping(@AuthenticationPrincipal JwtService.JwtPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "userId", principal.userId(),
                "email", principal.email()
        ));
    }
}

