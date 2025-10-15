package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.api.dto.thread.CreateThreadDto;
import com.sallyvnge.aipromptbackend.api.dto.thread.PageDto;
import com.sallyvnge.aipromptbackend.api.dto.thread.PatchThreadDto;
import com.sallyvnge.aipromptbackend.api.dto.thread.ThreadDto;
import com.sallyvnge.aipromptbackend.service.ThreadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/threads")
@RequiredArgsConstructor
public class ThreadController {
    private final ThreadService threadService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ThreadDto create(
            @RequestBody @Valid CreateThreadDto createThreadDto
            ) {
        return threadService.create(createThreadDto);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageDto<ThreadDto> list(
            @RequestParam Optional<Integer> limit,
            @RequestParam Optional<String> cursor
            ) {
        return threadService.list(limit.orElse(20), cursor.orElse(null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ThreadDto get(@PathVariable UUID id) {
        return threadService.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ThreadDto patch(
            @PathVariable UUID id,
            @RequestBody @Valid PatchThreadDto patchThreadDto
            ) {
        return threadService.patch(id, patchThreadDto);
    }
}
