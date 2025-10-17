package com.sallyvnge.aipromptbackend.service.memory.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.UserMemoryEntity;
import com.sallyvnge.aipromptbackend.infrastructure.persistence.repository.UserMemoryRepository;
import com.sallyvnge.aipromptbackend.service.memory.UserMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserMemoryServiceImpl implements UserMemoryService {

    private final UserMemoryRepository userMemoryRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getProfileText(UUID userId) {
        return userMemoryRepository.findById(userId)
                .map(UserMemoryEntity::getProfileJson)
                .map(this::jsonToCompactText)
                .orElse("");
    }

    @Override
    public void updateProfileJson(UUID userId, String json) {
        String safeJson = normalizeJson(json);
        UserMemoryEntity userMemory = userMemoryRepository.findById(userId).orElseGet(() -> {
                    UserMemoryEntity um = new UserMemoryEntity();
                    um.setUserId(userId);
                    return um;
                }
        );

        userMemory.setProfileJson(safeJson);
        userMemory.setUpdatedAt(Instant.now());
        userMemoryRepository.save(userMemory);

    }

    private String normalizeJson(String json) {
        try {
            JsonNode n = mapper.readTree(json == null ? "{}" : json);
            return mapper.writeValueAsString(n);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String jsonToCompactText(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            StringBuilder sb = new StringBuilder();
            flatten("", root, sb);
            return sb.toString().replaceAll(";\\s*$", "");
        } catch (Exception e) {
            return "";
        }
    }

    private void flatten(String prefix, JsonNode node, StringBuilder sb) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                var e = it.next();
                String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
                flatten(key, e.getValue(), sb);
            }
        } else if (node.isArray()) {
            int i = 0;
            for (JsonNode item : node) {
                flatten(prefix + "[" + i++ + "]", item, sb);
            }
        } else {
            sb.append(prefix).append('=').append(node.asText()).append("; ");
        }
    }
}
