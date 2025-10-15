package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.api.dto.message.CreateMessageDto;
import com.sallyvnge.aipromptbackend.api.dto.message.MessageDto;
import com.sallyvnge.aipromptbackend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/threads/{threadId}/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MessageDto> list(
            @PathVariable UUID threadId,
            @RequestParam Optional<Integer> afterPosition,
            @RequestParam Optional<Integer> limit
            ) {
        return messageService.list(threadId, afterPosition.orElse(-1), limit.orElse(50));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public MessageDto createUserMessage(
            @PathVariable UUID threadId,
            @RequestBody @Valid CreateMessageDto createMessageDto
            ) {
        return messageService.createUserMessage(threadId, createMessageDto);
    }
}
