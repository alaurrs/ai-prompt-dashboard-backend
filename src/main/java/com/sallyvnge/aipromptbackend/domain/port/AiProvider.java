package com.sallyvnge.aipromptbackend.domain.port;

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
}
