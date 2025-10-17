package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.security.JwtService;
import com.sallyvnge.aipromptbackend.service.memory.SemanticMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dev/memory")
@RequiredArgsConstructor
public class DevMemoryController {

    private final SemanticMemoryService semanticMemoryService;

    @GetMapping("/smoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> smoke(
            @AuthenticationPrincipal JwtService.JwtPrincipal principal,
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "k", required = false, defaultValue = "5") int k
    ) {
        UUID userId = principal.userId();

        String toIndex = (text == null || text.isBlank()) ? "The quick brown fox jumps over the lazy dog." : text;
        String q = (query == null || query.isBlank()) ? "fox" : query;

        semanticMemoryService.indexChunk(userId, null, "note", toIndex);

        List<String> results = semanticMemoryService.retrieve(userId, q, Math.max(1, Math.min(k, 10)));
        Map<String, Object> payload = new HashMap<>();
        payload.put("indexedChars", toIndex.length());
        payload.put("query", q);
        payload.put("results", results);

        return ResponseEntity.ok(payload);
    }
}

