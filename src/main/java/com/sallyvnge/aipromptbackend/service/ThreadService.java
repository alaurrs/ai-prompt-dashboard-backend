package com.sallyvnge.aipromptbackend.service;

import com.sallyvnge.aipromptbackend.api.dto.thread.*;
import com.sallyvnge.aipromptbackend.config.CurrentUserProvider;
import com.sallyvnge.aipromptbackend.domain.MessageEntity;
import com.sallyvnge.aipromptbackend.domain.ThreadEntity;
import com.sallyvnge.aipromptbackend.domain.TitleSource;
import com.sallyvnge.aipromptbackend.domain.UserEntity;
import com.sallyvnge.aipromptbackend.repository.MessageRepository;
import com.sallyvnge.aipromptbackend.repository.ThreadRepository;
import com.sallyvnge.aipromptbackend.repository.UserRepository;
import com.sallyvnge.aipromptbackend.service.SseHub;
import com.sallyvnge.aipromptbackend.domain.port.AiProvider;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThreadService {
    private final ThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SseHub sseHub;
    private final AiProvider aiProvider;

    @Value("${app.ai.model.summary:gpt-5-nano}")
    private String summaryModel;

    private UserEntity requireOrCreateUser() {
        var currentUser = currentUserProvider.require();
        return userRepository.findByEmail(currentUser.email()).orElseGet(() -> {
            UserEntity user = new UserEntity();
            user.setEmail(currentUser.email());
            user.setDisplayName(currentUser.displayName());
            return userRepository.save(user);
        });
    }

    @Transactional
    public ThreadDto createWithFirstMessage(CreateThreadWithMessageDto createThreadWithMessageDto) {
        UserEntity user = requireOrCreateUser();
        String model = (createThreadWithMessageDto.model() == null || createThreadWithMessageDto.model().isBlank()) ? "gpt-5-nano" : createThreadWithMessageDto.model();
        String provisionalTitle = computeHeuristicTitle(createThreadWithMessageDto.content());
        if (provisionalTitle == null || provisionalTitle.isBlank()) {
            provisionalTitle = "New chat";
        }

        ThreadEntity thread = ThreadEntity.builder()
                .user(user)
                .title(provisionalTitle)
                .titleSource(TitleSource.AUTO)
                .model(model)
                .systemPrompt(createThreadWithMessageDto.systemPrompt())
                .status("active")
                .build();
        thread = threadRepository.save(thread);
        MessageEntity message = MessageEntity.builder()
                .thread(thread)
                .author("user")
                .position(0)
                .status("complete")
                .content(createThreadWithMessageDto.content())
                .build();
        message = messageRepository.save(message);
        thread.setFirstMessage(message);
        thread.setUpdatedAt(Instant.now());
        thread = threadRepository.save(thread);

        UUID threadId = thread.getId();
        UUID userId = user.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refineTitleAsync(threadId, userId);
                }
            });
        }
        else {
            refineTitleAsync(threadId, userId);
        }
        return toDto(thread);

    }

    @Transactional
    public ThreadDto create(CreateThreadDto createThreadDto) {
        UserEntity user = requireOrCreateUser();
        String title = (createThreadDto.title() == null || createThreadDto.title().isBlank()) ? "New chat" : createThreadDto.title();
        String model = (createThreadDto.model() == null || createThreadDto.model().isBlank()) ? "gpt-5-nano" : createThreadDto.model();

        ThreadEntity thread = ThreadEntity.builder()
                .user(user)
                .title(title)
                .titleSource((createThreadDto.title() != null && !createThreadDto.title().isBlank()) ? TitleSource.USER : TitleSource.AUTO)
                .model(model)
                .systemPrompt(createThreadDto.systemPrompt())
                .status("active")
                .build();
        thread = threadRepository.save(thread);
        return toDto(thread);
    }

    @Transactional(readOnly = true)
    public PageDto<ThreadDto> list(int limit, String cursor) {
        Instant updatedBefore = null;
        if (cursor != null && !cursor.isBlank()) updatedBefore = Instant.parse(cursor);
        List<ThreadEntity> list = threadRepository.pageByUser(requireOrCreateUser(), null, updatedBefore, PageRequest.of(0, Math.min(limit, 50)));
        String next = list.isEmpty() ? null: list.getLast().getUpdatedAt().toString();
        return new PageDto<>(list.stream().map(this::toDto).toList(), next);
    }

    @Transactional(readOnly = true)
    public ThreadDto get(UUID id) {
        ThreadEntity thread = threadRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        ensureOwner(thread);
        return toDto(thread);
    }

    @Transactional
    public ThreadDto patch(UUID id, PatchThreadDto patchThreadDto) {
        ThreadEntity thread = threadRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        ensureOwner(thread);
        if (thread.getVersion() != patchThreadDto.version()) {
            throw new OptimisticLockException();
        }
        String previousTitle = thread.getTitle();
        boolean titleChanged = false;
        if (patchThreadDto.title() != null) {
            String incoming = patchThreadDto.title();
            titleChanged = (incoming != null && !incoming.equals(previousTitle));
            thread.setTitle(incoming);
            thread.setTitleSource(TitleSource.USER);
            thread.setUpdatedAt(Instant.now());
        }
        if (patchThreadDto.status() != null) thread.setStatus(patchThreadDto.status());
        if (patchThreadDto.model() != null) thread.setModel(patchThreadDto.model());
        if (patchThreadDto.systemPrompt() != null) thread.setSystemPrompt(patchThreadDto.systemPrompt());

        if (titleChanged) {
            UUID userId = thread.getUser().getId();
            UUID threadId = thread.getId();
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            ThreadEntity updated = threadRepository.findById(threadId).orElse(null);
                            if (updated != null) {
                                var payload = java.util.Map.of(
                                        "threadId", updated.getId(),
                                        "title", updated.getTitle(),
                                        "titleSource", "user",
                                        "updatedAt", updated.getUpdatedAt()
                                );
                                sseHub.emit(userId, "thread.title_updated", payload);
                            }
                        } catch (Exception ignored) {}
                    }
                });
            }
        }
        return toDto(thread);
    }

    private ThreadDto toDto(ThreadEntity thread) {
        return ThreadDto.builder()
                .id(thread.getId())
                .title(thread.getTitle())
                .titleSource(thread.getTitleSource())
                .status(thread.getStatus())
                .model(thread.getModel())
                .systemPrompt(thread.getSystemPrompt())
                .version(thread.getVersion())
                .summary(thread.getSummary())
                .createdAt(thread.getCreatedAt())
                .updatedAt(thread.getUpdatedAt())
                .build();
    }

    private void ensureOwner(ThreadEntity thread) {
        if (!thread.getUser().getId().equals(requireOrCreateUser().getId())) {
            throw new IllegalArgumentException("Forbidden");
        }
    }

    public void refineTitleAsync(UUID threadId, UUID userId) {
        Runnable task = () -> {
            try {
                ThreadEntity thread = threadRepository.findById(threadId).orElse(null);
                if (thread == null) return;
                if (thread.getTitleSource() == TitleSource.USER) {
                    return;
                }
                String firstContent = null;
                try {
                    if (thread.getFirstMessage() != null && thread.getFirstMessage().getContent() != null) {
                        firstContent = thread.getFirstMessage().getContent();
                    }
                } catch (Exception ignored) {}

                if(firstContent == null || firstContent.isBlank()) {
                    List<MessageEntity> page = messageRepository.findByThreadAndPositionGreaterThanOrderByPositionAsc(
                            thread, -1, PageRequest.of(0,1)
                    );
                    if (!page.isEmpty() && page.getFirst().getContent() != null) {
                        firstContent = page.getFirst().getContent();
                    }
                }

                if (firstContent != null && firstContent.isBlank()) {
                    return;
                }

                String candidate = null;
                try {
                    candidate = computeAiTitle(firstContent, thread.getModel(), thread.getSystemPrompt());
                } catch (Exception e) {
                    log.debug("AI title generation failed, falling back to heuristic: {}", e.getMessage());
                }
                if (candidate == null || candidate.isBlank()) {
                    candidate = computeHeuristicTitle(firstContent);
                }
                if (candidate == null || candidate.isBlank()) {
                    return;
                }

                boolean updated = applyTitleIfAllowed(threadId, candidate);
                if (updated) {
                    ThreadEntity updatedThread = threadRepository.findById(threadId).orElse(null);
                    if (updatedThread != null) {
                        var payload = java.util.Map.of(
                                "threadId", updatedThread.getId(),
                                "title", updatedThread.getTitle(),
                                "titleSource", "ai",
                                "updatedAt", updatedThread.getUpdatedAt()
                        );
                        sseHub.emit(userId, "thread.title_updated", payload);
                    }
                }

            } catch (Exception e) {
                log.warn("Title refinement failed for thread {}: {}", threadId, e.getMessage(), e);
            }
        };
        Thread t = new Thread(task, "refine-title-" + threadId);
        t.setDaemon(true);
        t.start();
    }

    private String computeAiTitle(String content, String threadModel, String threadSystemPrompt) {
        if (content == null || content.isBlank()) return null;

        String model = (threadModel != null && !threadModel.isBlank()) ? threadModel : summaryModel;
        String system = "You are an assistant that writes a concise, clear chat title in the user's language."
                + " Rules: <= 40 characters, no quotes, no markdown, no code fences, no trailing punctuation,"
                + " no surrounding punctuation, no emojis. Output ONLY the title text.";
        if (threadSystemPrompt != null && !threadSystemPrompt.isBlank()) {
            system = threadSystemPrompt + "\n\n" + system;
        }

        String user = "First user message:\n" + content + "\n\nReturn only the title:";

        String raw = aiProvider.completeOnce(model, system, user);
        if (raw == null) return null;
        String title = raw.replaceAll("[\r\n]+", " ").trim();
        title = title.replaceAll("^(?:[`\"\u00AB\u2039\u2018\u201C]+)|([`\"\u00BB\u203A\u2019\u201D]+)$", "").trim();
        title = title.replaceAll("\\s+", " ").trim();
        return title;
    }

    private String computeHeuristicTitle(String content) {
        if (content == null) return "Nouvelle conversation";
        String[] lines = content.split("(\\r\\n|\\n|\\r)");
        String first = null;
        for (String l : lines) {
            if (l != null && !l.trim().isBlank()) { first = l.trim(); break; }
        }
        String title = first == null ? "" : first;

        title = title.replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`[^`]*`", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[#*_~>]+", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (title.isBlank() || title.matches("^[;{}()\\[\\]<>=+\\-/*\\s]+$")) {
            title = "Nouvelle conversation";
        }

        int max = 80;
        if (title.length() > max) {
            int cut = title.lastIndexOf(' ', max);
            title = (cut > 0 ? title.substring(0, cut) : title.substring(0, max)).trim() + "…";
        }

        if (!title.isEmpty()) {
            title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
        }
        return title;
    }

    @Transactional
    boolean applyTitleIfAllowed(UUID threadId, String candidate) {
        if (candidate == null) return false;

        String title = candidate.trim().replaceAll("\\s+", " ");
        if (title.isEmpty()) return false;

        int max = 120;
        if (title.length() > max) {
            int cut = title.lastIndexOf(' ', max);
            title = (cut > 0 ? title.substring(0, cut) : title.substring(0, max)).trim() + "…";
        }

        try {
            ThreadEntity thread = threadRepository.findById(threadId).orElse(null);
            if (thread == null) return false;

            if (thread.getTitleSource() == TitleSource.USER) {
                return false;
            }

            if (title.equals(thread.getTitle())) {
                // If the AI confirms the provisional title, promote source to AI
                if (thread.getTitleSource() != TitleSource.AI) {
                    thread.setTitleSource(TitleSource.AI);
                    thread.setUpdatedAt(java.time.Instant.now());
                    threadRepository.save(thread);
                    return true;
                }
                return false;
            }

            thread.setTitle(title);
            thread.setTitleSource(TitleSource.AI);
            thread.setUpdatedAt(java.time.Instant.now());

            threadRepository.save(thread);
            return true;
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
