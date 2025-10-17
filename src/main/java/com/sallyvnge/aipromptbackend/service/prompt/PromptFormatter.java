package com.sallyvnge.aipromptbackend.service.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptFormatter {

    public String buildSystem(PromptContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.systemPromptBase() == null ? "" : context.systemPromptBase().trim());

        if (context.userProfileMemory() != null && !context.userProfileMemory().isBlank()) {
            sb.append("\n\n[User Profile]\n").append(context.userProfileMemory());
        }
        if (context.threadSummary() != null && !context.threadSummary().isBlank()) {
            sb.append("\n\n[Thread Summary]\n").append(context.threadSummary());
        }
        if (context.recentConversation() != null && !context.recentConversation().isBlank()) {
            sb.append("\n\n[Recent Conversation]\n").append(context.recentConversation());
        }
        List<String> notes = context.semanticSnippets();
        if (notes != null && !notes.isEmpty()) {
            sb.append("\n\n[Relevant Notes]\n");
            for (String n : notes) {
                sb.append("- ").append(n).append("\n");
            }
        }
        return sb.toString().trim();
    }

    public String buildUser(String userPrompt) {
        return userPrompt == null ? "" : userPrompt;
    }
}
