package com.sallyvnge.aipromptbackend.domain.port;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface AiProvider {
    void respondStream(
            String model,
            String systemPrompt,
            String userPrompt,
            Consumer<String> onDelta,
            Runnable onDone,
            Consumer<Throwable> onError
    );

    default String completeOnce(String model, String systemPrompt, String userPrompt) {
        return completeOnce(model, systemPrompt, userPrompt, Duration.ofSeconds(60));
    }

    default String completeOnce(String model, String systemPrompt, String userPrompt, Duration timeout) {
        StringBuilder sb = new StringBuilder();
        CompletableFuture<String> done = new CompletableFuture<String>();
        AtomicBoolean finished = new AtomicBoolean(false);

        respondStream(
                model,
                systemPrompt,
                userPrompt,
                delta -> {
                    if (!finished.get()) sb.append(delta);
                },
                () -> {
                    if (finished.compareAndSet(false, true)) {
                        done.complete(sb.toString());
                    }
                },
                err -> {
                    if (finished.compareAndSet(false, true)) {
                        done.completeExceptionally(err);
                    }
                }
        );

        try {
            return done.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("completeOnce timed out or failed", e);
        }
    }
}
