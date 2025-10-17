package com.sallyvnge.aipromptbackend.service.prompt;

import java.util.List;

public record PromptContext(
        String systemPromptBase,
        String userProfileMemory,
        String threadSummary,
        List<String> semanticSnippets,
        String recentConversation
) { }
