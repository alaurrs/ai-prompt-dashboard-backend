package com.sallyvnge.aipromptbackend.service.memory;

public final class TokenEstimator {
    private TokenEstimator() {
    }

    public static int estimateTokens(String s) {
        if (s == null || s.isEmpty()) return 0;

        return Math.max(1, s.length() / 4);
    }
}
