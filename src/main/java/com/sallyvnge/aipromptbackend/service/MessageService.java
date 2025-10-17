package com.sallyvnge.aipromptbackend.service;

import com.sallyvnge.aipromptbackend.api.dto.message.CreateMessageDto;
import com.sallyvnge.aipromptbackend.api.dto.message.MessageDto;
import com.sallyvnge.aipromptbackend.domain.MessageEntity;
import com.sallyvnge.aipromptbackend.domain.ThreadEntity;
import com.sallyvnge.aipromptbackend.repository.MessageRepository;
import com.sallyvnge.aipromptbackend.repository.ThreadRepository;
import com.sallyvnge.aipromptbackend.service.memory.EpisodeExtractor;
import com.sallyvnge.aipromptbackend.service.memory.SemanticMemoryService;
import com.sallyvnge.aipromptbackend.service.memory.ThreadSummaryService;
import com.sallyvnge.aipromptbackend.service.prompt.PromptAssembler;
import com.sallyvnge.aipromptbackend.service.prompt.PromptContext;
import com.sallyvnge.aipromptbackend.service.prompt.PromptFormatter;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    private final ThreadRepository threadRepository;
    private final ThreadSummaryService threadSummaryService;
    private final SemanticMemoryService semanticMemoryService;
    private final MessageRepository messageRepository;
    private final EpisodeExtractor episodeExtractor;
    private final PromptAssembler promptAssembler;
    private final PromptFormatter promptFormatter;

    @Transactional(readOnly = true)
    public List<MessageDto> list(UUID threadId, int afterPosition, int limit) {
        ThreadEntity thread = threadRepository.findById(threadId).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        List<MessageEntity> page = messageRepository.findByThreadAndPositionGreaterThanOrderByPositionAsc(thread, Math.max(afterPosition, -1), PageRequest.of(0, Math.min(limit, 100)));
        return page.stream().map(
                this::toDto
        ).toList();
    }

    @Transactional
    public MessageDto createUserMessage(UUID threadId, CreateMessageDto createMessageDto) {
        if (!"user".equals(createMessageDto.author())) throw new IllegalArgumentException("author must be 'user'");
        ThreadEntity thread = threadRepository.findById(threadId).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        int nextPos = messageRepository.findTopByThreadOrderByPositionDesc(thread).map(m -> m.getPosition() + 1).orElse(0);
        MessageEntity message = MessageEntity.builder()
                .thread(thread)
                .author("user")
                .position(nextPos)
                .status("complete")
                .content(createMessageDto.content())
                .build();
        message = messageRepository.save(message);
        return toDto(message);
    }

    @Transactional
    public MessageDto createAssistantDraft(UUID threadId) {
        ThreadEntity thread = threadRepository.findById(threadId).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        int nextPos = messageRepository.findTopByThreadOrderByPositionDesc(thread).map(m -> m.getPosition() +1).orElse(0);

        MessageEntity message = MessageEntity.builder()
                .thread(thread)
                .author("assistant")
                .position(nextPos)
                .status("streaming")
                .content("null")
                .model(thread.getModel())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        message = messageRepository.save(message);

        thread.setUpdatedAt(Instant.now());

        return toDto(message);
    }

    @Transactional
    public void appendAssistantDelta(UUID messageId, String delta) {
        messageRepository.appendDelta(messageId, delta);
    }

    @Transactional
    public MessageDto finalizeAssistantMessage(UUID messageId, UUID threadId, @Nullable String latestUserMsg) {
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!"streaming".equals(message.getStatus())) {
            return toDto(message);
        }

        String finalContent = Optional.ofNullable(message.getContentDelta()).orElse("");
        message.setContent(finalContent);
        message.setContentDelta(null);
        message.setStatus("complete");
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);

        UUID userId = threadRepository.findOwnerIdByThreadId(threadId).orElseThrow(
                () -> new IllegalArgumentException("Owner not found")
        );

        String u = Optional.ofNullable(latestUserMsg).orElse("");
        String a = finalContent;

        try { threadSummaryService.updateSummary(threadId, u, a); } catch (Exception ignored) {}
        try { semanticMemoryService.indexChunk(userId, threadId, "assistant", a); } catch (Exception ignored) {}
        try { episodeExtractor.maybeCreateEpisodes(userId, threadId, (u + " " + a).trim()); } catch (Exception ignored) {}

        return toDto(message);
    }

    @Transactional
    public MessageDto finalizeAssistantMessage(
            UUID messageId,
            UUID userId,
            UUID threadId,
            @Nullable String latestUserMsg,
            @Nullable String assistantMsgComplete
    ) {
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!"streaming".equals(message.getStatus())) {
            return toDto(message);
        }

        String finalContent = Optional.ofNullable(message.getContentDelta()).orElse("");
        message.setContent(finalContent);
        message.setContentDelta(null);
        message.setStatus("complete");
        message.setUpdatedAt(Instant.now());

        messageRepository.save(message);

        String u = Optional.ofNullable(latestUserMsg).orElse("");
        String a = Optional.ofNullable(assistantMsgComplete).orElse(finalContent);

        try {

            threadSummaryService.updateSummary(threadId, u, a);
        } catch (Exception e) {
            log.warn("Summary update failed for thread {}: {}", threadId, e.getMessage(), e);
        }

        try {
            semanticMemoryService.indexChunk(userId, threadId, "assistant", a);
        } catch (Exception e) {
            log.warn("Semantic index failed for thread {}: {}", threadId, e.getMessage(), e);
        }

        try {
            episodeExtractor.maybeCreateEpisodes(userId, threadId, (u + " " + a).trim());
        } catch (Exception e) {
            log.warn("Episode extraction failed for thread {}: {}", threadId, e.getMessage(), e);
        }

        return toDto(message);
    }

    /**
     * Finalize the assistant message quickly (persist final content and mark complete),
     * without running post-processing (summary, semantic index, episodes).
     * Intended for low-latency SSE completion; post-processing can be run asynchronously.
     */
    @Transactional
    public MessageDto finalizeAssistantMessageQuick(UUID messageId) {
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!"streaming".equals(message.getStatus())) {
            return toDto(message);
        }

        String finalContent = Optional.ofNullable(message.getContentDelta()).orElse("");
        message.setContent(finalContent);
        message.setContentDelta(null);
        message.setStatus("complete");
        message.setUpdatedAt(Instant.now());

        messageRepository.save(message);

        return toDto(message);
    }

    /**
     * Run post-finalization tasks in background to avoid blocking SSE completion.
     */
    public void postFinalizeAssistantTasksAsync(UUID userId, UUID threadId, String latestUserMsg, String assistantMsgComplete) {
        Runnable task = () -> {
            try {
                threadSummaryService.updateSummary(threadId, latestUserMsg, assistantMsgComplete);
            } catch (Exception e) {
                log.warn("Summary update failed for thread {}: {}", threadId, e.getMessage(), e);
            }

            try {
                semanticMemoryService.indexChunk(userId, threadId, "assistant", assistantMsgComplete);
            } catch (Exception e) {
                log.warn("Semantic index failed for thread {}: {}", threadId, e.getMessage(), e);
            }

            try {
                episodeExtractor.maybeCreateEpisodes(userId, threadId, (latestUserMsg + " " + assistantMsgComplete).trim());
            } catch (Exception e) {
                log.warn("Episode extraction failed for thread {}: {}", threadId, e.getMessage(), e);
            }
        };

        Thread t = new Thread(task, "post-finalize-" + threadId);
        t.setDaemon(true);
        t.start();
    }


    @Transactional
    public void failAssistantMessage(UUID messageId, String errorMessage, String errorCode) {
        messageRepository.markError(messageId, errorCode, errorMessage);
    }

    public PreparedCall prepareProviderInputs(UUID userId, UUID threadId, String userPrompt, String systemPromptBase) {
        PromptContext context = promptAssembler.assemble(userId, threadId, userPrompt, systemPromptBase);
        String system = promptFormatter.buildSystem(context);
        String user = promptFormatter.buildUser(userPrompt);
        return new PreparedCall(system, user);
    }

    private MessageDto toDto(MessageEntity message) {
        return MessageDto.builder()
                .id(message.getId())
                .threadId(message.getThread().getId())
                .author(message.getAuthor())
                .position(message.getPosition())
                .content(message.getContent())
                .contentDelta(message.getContentDelta())
                .status(message.getStatus())
                .model(message.getModel())
                .usageCompletionTokens(message.getUsageCompletionTokens())
                .usagePromptTokens(message.getUsagePromptTokens())
                .author(message.getAuthor())
                .latencyMs(message.getLatencyMs())
                .createdAt(message.getCreatedAt())
                .build();
    }
    public record PreparedCall(String system, String user) {}
}
