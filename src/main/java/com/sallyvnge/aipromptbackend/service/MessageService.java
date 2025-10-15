package com.sallyvnge.aipromptbackend.service;

import com.sallyvnge.aipromptbackend.api.dto.message.CreateMessageDto;
import com.sallyvnge.aipromptbackend.api.dto.message.MessageDto;
import com.sallyvnge.aipromptbackend.domain.MessageEntity;
import com.sallyvnge.aipromptbackend.domain.ThreadEntity;
import com.sallyvnge.aipromptbackend.repository.MessageRepository;
import com.sallyvnge.aipromptbackend.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final ThreadRepository threadRepository;
    private final MessageRepository messageRepository;

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
    public MessageDto finalizeAssistantMessage(UUID messageId) {
        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!"streaming".equals(message.getStatus())) return toDto(message);

        String finalContent = Optional.ofNullable(message.getContentDelta()).orElse("");
        message.setContent(finalContent);
        message.setContentDelta(null);
        message.setStatus("complete");
        message.setUpdatedAt(Instant.now());

        return toDto(message);
    }

    @Transactional
    public void failAssistantMessage(UUID messageId, String errorMessage, String errorCode) {
        messageRepository.markError(messageId, errorCode, errorMessage);
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
}
