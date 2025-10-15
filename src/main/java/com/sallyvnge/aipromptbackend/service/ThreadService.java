package com.sallyvnge.aipromptbackend.service;

import com.sallyvnge.aipromptbackend.api.dto.thread.CreateThreadDto;
import com.sallyvnge.aipromptbackend.api.dto.thread.PageDto;
import com.sallyvnge.aipromptbackend.api.dto.thread.PatchThreadDto;
import com.sallyvnge.aipromptbackend.api.dto.thread.ThreadDto;
import com.sallyvnge.aipromptbackend.config.CurrentUserProvider;
import com.sallyvnge.aipromptbackend.domain.ThreadEntity;
import com.sallyvnge.aipromptbackend.domain.UserEntity;
import com.sallyvnge.aipromptbackend.repository.ThreadRepository;
import com.sallyvnge.aipromptbackend.repository.UserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThreadService {
    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    private UserEntity requireOrCreateUser() {
        var currentUser = currentUserProvider.require();
        return userRepository.findByEmail(currentUser.email()).orElseGet(() -> {
            UserEntity user = new UserEntity();
            user.setEmail(currentUser.email());
            user.setDisplayName(currentUser.displayName());
            return userRepository.save(user);
        });
    }

    @Transactional
    public ThreadDto create(CreateThreadDto createThreadDto) {
        ThreadEntity thread = ThreadEntity.builder()
                .user(requireOrCreateUser())
                .title(Objects.requireNonNull(createThreadDto.title(), "New chat"))
                .model(Objects.requireNonNull(createThreadDto.model(), "o4-mini"))
                .systemPrompt(createThreadDto.systemPrompt())
                .status("active")
                .build();
        thread = threadRepository.save(thread);
        return toDto(thread);
    }

    @Transactional(readOnly = true)
    public PageDto<ThreadDto> list(int limit, String cursor) {
        Instant updatedBefore = null;
        if (cursor != null && !cursor.isBlank()) updatedBefore = Instant.parse(cursor);
        List<ThreadEntity> list = threadRepository.pageByUser(requireOrCreateUser(), null, updatedBefore, PageRequest.of(0, Math.min(limit, 50)));
        String next = list.isEmpty() ? null: list.getLast().getUpdatedAt().toString();
        return new PageDto<>(list.stream().map(this::toDto).toList(), next);
    }

    @Transactional(readOnly = true)
    public ThreadDto get(UUID id) {
        ThreadEntity thread = threadRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        ensureOwner(thread);
        return toDto(thread);
    }

    @Transactional
    public ThreadDto patch(UUID id, PatchThreadDto patchThreadDto) {
        ThreadEntity thread = threadRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        ensureOwner(thread);
        if (thread.getVersion() != patchThreadDto.version()) {
            throw new OptimisticLockException();
        }
        if (patchThreadDto.title() != null) thread.setTitle(patchThreadDto.title());
        if (patchThreadDto.status() != null) thread.setStatus(patchThreadDto.status());
        if (patchThreadDto.model() != null) thread.setModel(patchThreadDto.model());
        if (patchThreadDto.systemPrompt() != null) thread.setSystemPrompt(patchThreadDto.systemPrompt());
        return toDto(thread);
    }

    private ThreadDto toDto(ThreadEntity thread) {
        return ThreadDto.builder()
                .id(thread.getId())
                .title(thread.getTitle())
                .status(thread.getStatus())
                .model(thread.getModel())
                .systemPrompt(thread.getSystemPrompt())
                .createdAt(thread.getCreatedAt())
                .updatedAt(thread.getUpdatedAt())
                .build();
    }

    private void ensureOwner(ThreadEntity thread) {
        if (!thread.getUser().getId().equals(requireOrCreateUser().getId())) {
            throw new IllegalArgumentException("Forbidden");
        }
    }
}
