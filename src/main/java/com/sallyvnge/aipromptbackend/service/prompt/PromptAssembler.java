package com.sallyvnge.aipromptbackend.service.prompt;

import com.sallyvnge.aipromptbackend.config.AppMemoryProperties;
import com.sallyvnge.aipromptbackend.domain.MessageEntity;
import com.sallyvnge.aipromptbackend.repository.MessageRepository;
import com.sallyvnge.aipromptbackend.service.memory.SemanticMemoryService;
import com.sallyvnge.aipromptbackend.service.memory.ThreadSummaryService;
import com.sallyvnge.aipromptbackend.service.memory.UserMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromptAssembler {

    private final UserMemoryService userMemoryService;
    private final ThreadSummaryService threadSummaryService;
    private final SemanticMemoryService semanticMemoryService;
    private final MessageRepository messageRepository;
    private final AppMemoryProperties memoryProps;

    public PromptContext assemble(UUID userId, UUID threadId, String userPrompt, String systemPromptBase) {
        String profile = safe(userMemoryService.getProfileText(userId));
        String summary = safe(threadSummaryService.getSummary(threadId));
        List<String> snippets = semanticMemoryService.retrieve(userId, safe(userPrompt), 5);

        String recent = buildRecentConversation(threadId, memoryProps.getSummary().getTailMessages());

        return new PromptContext(systemPromptBase, profile, summary, snippets, recent);
    }

    private String buildRecentConversation(UUID threadId, int tail) {
        List<MessageEntity> desc = messageRepository.findByThread_IdOrderByPositionDesc(threadId, PageRequest.of(0, Math.max(1, Math.min(tail * 2, 50))));
        List<MessageEntity> list = new ArrayList<>(desc);
        Collections.reverse(list);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (MessageEntity m : list) {
            if (m.getContent() == null || m.getContent().isBlank() || "null".equals(m.getContent())) continue;
            if (!"user".equals(m.getAuthor()) && !"assistant".equals(m.getAuthor())) continue;
            sb.append("- [").append(m.getAuthor()).append("] ")
                    .append(truncate(m.getContent(), 1000)).append("\n");
            if (++count >= tail) break;
        }
        return sb.isEmpty() ? "" : sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
