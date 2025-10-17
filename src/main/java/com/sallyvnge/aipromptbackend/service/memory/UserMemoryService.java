package com.sallyvnge.aipromptbackend.service.memory;

import java.util.UUID;

public interface UserMemoryService {
    String getProfileText(UUID userId);
    void updateProfileJson(UUID userId, String json);
}
