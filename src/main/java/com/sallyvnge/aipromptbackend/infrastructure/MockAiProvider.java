package com.sallyvnge.aipromptbackend.infrastructure;

import com.sallyvnge.aipromptbackend.domain.port.AiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiProvider implements AiProvider {
    @Override
    public void respondStream(String model, String systemPrompt, String userPrompt,
                              Consumer<String> onDelta, Runnable onDone, Consumer<Throwable> onError) {
        try {
            for (String part : List.of("Bonjour ", "depuis ", "le mock ", "SSE !")) {
                onDelta.accept(part);
                Thread.sleep(300);
            }
            onDone.run();
        } catch (Exception e) {
            onError.accept(e);
        }
    }
}
