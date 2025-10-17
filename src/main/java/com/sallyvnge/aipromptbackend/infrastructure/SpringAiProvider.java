package com.sallyvnge.aipromptbackend.infrastructure;

import com.sallyvnge.aipromptbackend.domain.port.AiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "springai", matchIfMissing = false)
public class SpringAiProvider implements AiProvider {

    private final OpenAiChatModel openAiChatModel;

    @Override
    public void respondStream(
            String model, String systemPrompt, String userPrompt,
            Consumer<String> onDelta, Runnable onDone, Consumer<Throwable> onError) {

        var options = OpenAiChatOptions.builder().model(model).build();
        var prompt  = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                options
        );

        // Capture the current SecurityContext to propagate into Reactor callbacks
        final SecurityContext captured = SecurityContextHolder.getContext();

        Consumer<String> onDeltaCtx = delta -> withSecurityContext(captured, () -> onDelta.accept(delta));
        Runnable onDoneCtx = () -> withSecurityContext(captured, onDone);
        Consumer<Throwable> onErrorCtx = err -> withSecurityContext(captured, () -> onError.accept(err));

        openAiChatModel.stream(prompt)
                .map(resp -> resp.getResult().getOutput())
                .doOnNext(chunk -> { if (chunk != null) onDeltaCtx.accept(chunk.getText()); })
                .doOnComplete(onDoneCtx)
                .doOnError(onErrorCtx)
                .subscribe();
    }

    private static void withSecurityContext(SecurityContext ctx, Runnable action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(ctx);
            action.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
