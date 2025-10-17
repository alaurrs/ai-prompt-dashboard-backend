package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.api.dto.memory.EpisodeDto;
import com.sallyvnge.aipromptbackend.api.dto.memory.ProfileResponse;
import com.sallyvnge.aipromptbackend.api.dto.memory.ProfileUpdateRequest;
import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.MemoryEpisodeEntity;
import com.sallyvnge.aipromptbackend.infrastructure.persistence.repository.ThreadSummaryRepository;
import com.sallyvnge.aipromptbackend.service.memory.EpisodeService;
import com.sallyvnge.aipromptbackend.service.memory.UserMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MemoryController {

    private final UserMemoryService userMemoryService;
    private final EpisodeService episodeService;

    @GetMapping("/profile")
    public ProfileResponse getProfile(@RequestParam UUID userId) {
        String json = userMemoryService.getProfileText(userId).isBlank() ? "{}" : userMemoryService.getProfileText(userId);
        return new ProfileResponse("{}", json);
    }

    @PutMapping("/profile")
    public void updateProfile(@RequestParam UUID userId, @RequestBody ProfileUpdateRequest req) {
        userMemoryService.updateProfileJson(userId, req.json());
    }

    @GetMapping("/episodes")
    public List<EpisodeDto> getEpisodes(
            @RequestParam(required = false) UUID threadId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam UUID userId
    ) {
        List<MemoryEpisodeEntity> list = (threadId != null) ? episodeService.latestForThread(threadId, limit)
                : episodeService.latestForUser(userId, limit);

        return list.stream()
                .map(e -> new EpisodeDto(e.getId(), e.getTitle(), e.getDetail(), e.getOccurredAt()))
                .toList();
    }

    @DeleteMapping("/episodes/{episodeId}")
    public void deleteEpisode(@PathVariable UUID episodeId, @RequestParam UUID userId) {
        episodeService.delete(episodeId, userId);
    }
}
