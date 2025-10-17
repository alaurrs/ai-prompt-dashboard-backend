package com.sallyvnge.aipromptbackend.api.controller;

import com.sallyvnge.aipromptbackend.api.dto.message.MessageDto;
import com.sallyvnge.aipromptbackend.api.dto.message.RespondRequest;
import com.sallyvnge.aipromptbackend.domain.port.AiProvider;
import com.sallyvnge.aipromptbackend.security.JwtService;
import com.sallyvnge.aipromptbackend.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/threads")
@RequiredArgsConstructor
@Slf4j
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
        final String sys = request.systemPrompt();
        MessageDto draft = messageService.createAssistantDraft(threadId);
        final String model = (request.model() == null || request.model().isBlank()) ? draft.model() : request.model();
        final String user = request.prompt() == null ? "" : request.prompt();

        UUID currentUserId = principal.userId();
        MessageService.PreparedCall prepared = messageService.prepareProviderInputs(currentUserId, threadId, user, sys);

        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> log.debug("SSE completed for thread {}", threadId));
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for thread {}", threadId);
            try { send(emitter, "error", "timeout"); } catch (Exception ignored) {}
            try { emitter.complete(); } catch (Exception ignored) {}
        });
        emitter.onError(err -> {
            log.debug("SSE transport error for thread {}: {}", threadId, err == null ? "unknown" : err.getMessage());
            try { emitter.complete(); } catch (Exception ignored) {}
        });

        Runnable task = () -> {
            try {
                aiProvider.respondStream(
                        model, prepared.system(), prepared.user(),
                        delta -> {
                            messageService.appendAssistantDelta(draft.id(), delta);
                            send(emitter, "token", delta);
                        },
                        () -> {
                            MessageDto finalized = messageService.finalizeAssistantMessageQuick(draft.id());
                            send(emitter, "done", "[DONE]");
                            try { emitter.complete(); } catch (Exception ignored) {}
                            String assistantText = (finalized.content() == null) ? "" : finalized.content();
                            messageService.postFinalizeAssistantTasksAsync(
                                    principal.userId(), threadId, user, assistantText
                            );
                        },
                        err -> {
                            String message = (err == null || err.getMessage() == null || err.getMessage().isBlank())
                                    ? "provider_error" : err.getMessage();
                            messageService.failAssistantMessage(draft.id(), message, "PROVIDER_ERROR");
                            send(emitter, "error", message);
                            try { emitter.complete(); } catch (Exception ignored) {}
                        }
                );
            } catch (Exception e) {
                String message = (e.getMessage() == null || e.getMessage().isBlank()) ? "unexpected_error" : e.getMessage();
                messageService.failAssistantMessage(draft.id(), message, "UNEXPECTED");
                send(emitter, "error", message);
                try { emitter.complete(); } catch (Exception ignored) {}
            }
        };

        Thread thread = new Thread(new DelegatingSecurityContextRunnable(task, SecurityContextHolder.getContext()),
                "ai-respond-" + draft.id());

        thread.setDaemon(true);
        thread.start();

        return emitter;
    }

    private void send(SseEmitter sse, String event, String data) {
        try {
            sse.send(SseEmitter.event().name(event).data(data));
        } catch (Exception ignored) {  }
    }
}
