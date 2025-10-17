package com.sallyvnge.aipromptbackend.infrastructure.persistence.repository;

import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.ThreadSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ThreadSummaryRepository extends JpaRepository<ThreadSummaryEntity, UUID> {
}
