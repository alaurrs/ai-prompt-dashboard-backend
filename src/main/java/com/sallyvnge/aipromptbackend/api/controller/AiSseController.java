package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.api.dto.message.MessageDto;
import com.sallyvnge.aipromptbackend.api.dto.message.RespondRequest;
import com.sallyvnge.aipromptbackend.domain.port.AiProvider;
import com.sallyvnge.aipromptbackend.security.JwtService;
import com.sallyvnge.aipromptbackend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
            @RequestBody RespondRequest request,
            @AuthenticationPrincipal JwtService.JwtPrincipal principal
    ) {
        return respondInternal(threadId, request, principal);
    }

    @GetMapping(value = "/{threadId}/respond", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter respondGet(
            @PathVariable UUID threadId,
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "systemPrompt", required = false) String systemPrompt,
            @AuthenticationPrincipal JwtService.JwtPrincipal principal
    ) {
        RespondRequest req = new RespondRequest(prompt, model, systemPrompt);
        return respondInternal(threadId, req, principal);
    }

    private SseEmitter respondInternal(UUID threadId, RespondRequest request, JwtService.JwtPrincipal principal) {
        SseEmitter emitter = new SseEmitter(0L);
        final String sys = request.systemPrompt();
        MessageDto draft = messageService.createAssistantDraft(threadId);
        final String model = (request.model() == null || request.model().isBlank()) ? draft.model() : request.model();
        final String user = request.prompt() == null ? "" : request.prompt();

        UUID currentUserId = principal.userId();

        MessageService.PreparedCall prepared = messageService.prepareProviderInputs(currentUserId, threadId, user, sys);

        final SecurityContext parentContext = SecurityContextHolder.getContext();

        Thread thread = new Thread(() -> {
            try {
                SecurityContextHolder.setContext(parentContext);

                aiProvider.respondStream(
                        model, prepared.system(), prepared.user(),
                        delta -> {
                            messageService.appendAssistantDelta(draft.id(), delta);
                            send(emitter, "token", delta);
                        },
                        () -> {
                            messageService.finalizeAssistantMessage(
                                    draft.id(),
                                    principal.userId(),
                                    threadId,
                                    user,
                                    null
                            );
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
            } finally {
                SecurityContextHolder.clearContext();
            }
        }, "ai-respond-" + draft.id());

        thread.setDaemon(true);
        thread.start();

        return emitter;
    }

    private void send(SseEmitter sse, String event, String data) {
        try {
            sse.send(SseEmitter.event().name(event).data(data));
        } catch (Exception ignored) { /* client disconnected */ }
    }
}
