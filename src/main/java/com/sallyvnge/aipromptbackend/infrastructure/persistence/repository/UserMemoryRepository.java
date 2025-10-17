package com.sallyvnge.aipromptbackend.infrastructure.persistence.repository;

import com.sallyvnge.aipromptbackend.infrastructure.persistence.entity.UserMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserMemoryRepository extends JpaRepository<UserMemoryEntity, UUID> {
}
