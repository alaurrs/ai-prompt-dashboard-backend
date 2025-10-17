package com.sallyvnge.aipromptbackend.api.dto.memory;

public record SummaryDto(
        String summaryText, int tokensEstimated
) {
}
