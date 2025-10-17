package com.sallyvnge.aipromptbackend.service.memory.impl;

import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.MemoryEpisodeEntity;
import com.sallyvnge.aipromptbackend.infrastructure.persistence.repository.MemoryEpisodeRepository;
import com.sallyvnge.aipromptbackend.service.memory.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EpisodeServiceImpl implements EpisodeService {

    private final MemoryEpisodeRepository memoryEpisodeRepository;

    @Override
    @Transactional
    public MemoryEpisodeEntity create(UUID userId, UUID threadId, String title, String detail) {
        MemoryEpisodeEntity episode = new MemoryEpisodeEntity();
        episode.setId(UUID.randomUUID());
        episode.setUserId(userId);
        episode.setThreadId(threadId);
        episode.setOccurredAt(Instant.now());
        episode.setTitle(title);
        episode.setDetail(detail);
        episode.setCreatedAt(Instant.now());
        return memoryEpisodeRepository.save(episode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEpisodeEntity> latestForUser(UUID userId, int limit) {
        List<MemoryEpisodeEntity> list = memoryEpisodeRepository.findTop20ByUserIdOrderByOccurredAtDesc(userId);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEpisodeEntity> latestForThread(UUID threadId, int limit) {
        List<MemoryEpisodeEntity> list = memoryEpisodeRepository.findTop20ByThreadIdOrderByOccurredAtDesc(threadId);
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    @Override
    @Transactional
    public void delete(UUID episodeId, UUID userId) {
        MemoryEpisodeEntity episode = memoryEpisodeRepository.findById(episodeId).orElseThrow(() -> new IllegalArgumentException("Episode not found"));
        if (!episode.getUserId().equals(userId)) throw new IllegalArgumentException("Episode not found");
        memoryEpisodeRepository.delete(episode);
    }
}
