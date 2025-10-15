package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.api.dto.message.MessageDto;
import com.sallyvnge.aipromptbackend.api.dto.message.RespondRequest;
import com.sallyvnge.aipromptbackend.domain.port.AiProvider;
import com.sallyvnge.aipromptbackend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/threads")
@RequiredArgsConstructor
public class AiSseController {

    private final MessageService messageService;
    private final AiProvider aiProvider;

    @PostMapping(value = "/{threadId}/respond", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter respond(
            @PathVariable UUID threadId,
            @RequestBody RespondRequest request
    ) {
        SseEmitter emitter = new SseEmitter(0L);
        MessageDto draft = messageService.createAssistantDraft(threadId);
        final String model = (request.model() == null || request.model().isBlank()) ? draft.model() : request.model();
        final String sys = request.systemPrompt();
        final String user = request.prompt() == null ? "" : request.prompt();


        new Thread(() -> {
            try {
                aiProvider.respondStream(
                        model, sys, user,
                        delta -> {
                            messageService.appendAssistantDelta(draft.id(), delta);
                            send(emitter, "token", delta);
                        },
                        () -> {
                            messageService.finalizeAssistantMessage(draft.id());
                            send(emitter, "done", "[DONE]");
                            emitter.complete();
                        },
                        err -> {
                            messageService.failAssistantMessage(draft.id(), err.getMessage(), "PROVIDER_ERROR");
                            send(emitter, "error", err.getMessage());
                            emitter.completeWithError(err);
                        }
                );
            } catch (Exception e) {
                messageService.failAssistantMessage(draft.id(), e.getMessage(), "UNEXPECTED");
                send(emitter, "error", e.getMessage());
                emitter.completeWithError(e);
            }
        }, "ai-respond-" + draft.id()).start();

        return emitter;
    }

    private void send(SseEmitter sse, String event, String data) {
        try {
            sse.send(SseEmitter.event().name(event).data(data));
        } catch (Exception ignored) { /* client disconnected */ }
    }
}
