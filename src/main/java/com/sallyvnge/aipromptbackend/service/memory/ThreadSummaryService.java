package com.sallyvnge.aipromptbackend.service.memory;

import java.util.UUID;

public interface ThreadSummaryService {
    String getSummary(UUID threadId);
    void updateSummary(UUID threadId, String latestUserMsg, String latestAssistantMsg);
}
