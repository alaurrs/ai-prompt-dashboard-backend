package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.config.CurrentUserProvider;
import com.sallyvnge.aipromptbackend.domain.UserEntity;
import com.sallyvnge.aipromptbackend.repository.UserRepository;
import com.sallyvnge.aipromptbackend.service.SseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final SseHub sseHub;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping(path = "", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter subscribe() {
        var current = currentUserProvider.require();
        UserEntity user = userRepository.findByEmail(current.email()).orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setEmail(current.email());
            u.setDisplayName(current.displayName());
            return userRepository.save(u);
        });
        return sseHub.subscribe(user.getId());
    }
}

