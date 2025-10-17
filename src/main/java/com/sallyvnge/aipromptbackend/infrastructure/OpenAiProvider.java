package com.sallyvnge.aipromptbackend.infrastructure;


import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.*;
import com.sallyvnge.aipromptbackend.domain.port.AiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai", matchIfMissing = false)
public class OpenAiProvider implements AiProvider {

    private final OpenAIClient client;

    @Override
    public void respondStream(
            String model,
            String systemPrompt,
            String userPrompt,
            Consumer<String> onDelta,
            Runnable onDone,
            Consumer<Throwable> onError
    ) {
        // fallback modèle si null/blank
        final String effectiveModel = (model == null || model.isBlank()) ? "gpt-4o-mini" : model;

        try {
            // 1) construire la liste des messages
            ChatCompletionCreateParams.Builder paramsBuilder = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of(effectiveModel))
                    .temperature(1);

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                paramsBuilder.addMessage(ChatCompletionSystemMessageParam.builder()
                        .content(systemPrompt)
                        .build());
            }

            paramsBuilder.addMessage(ChatCompletionUserMessageParam.builder()
                    .content(userPrompt)
                    .build());

            ChatCompletionCreateParams params = paramsBuilder.build();

            // 2) ouvrir le flux et émettre les deltas
            try (StreamResponse<ChatCompletionChunk> stream =
                         client.chat().completions().createStreaming(params)) {

                stream.stream().forEach(chunk ->
                    chunk.choices().forEach(choice ->
                        choice.delta().content().ifPresent(content -> {
                            if (!content.isEmpty()) {
                                onDelta.accept(content);
                            }
                        })
                    )
                );

                // 3) fin de flux
                onDone.run();
            }
        } catch (Exception e) {
            log.warn("OpenAI stream error", e);
            onError.accept(e);
        }
    }
}