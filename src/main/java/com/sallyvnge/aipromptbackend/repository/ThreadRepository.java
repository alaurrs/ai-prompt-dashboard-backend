package com.sallyvnge.aipromptbackend.repository;

import com.sallyvnge.aipromptbackend.domain.ThreadEntity;
import com.sallyvnge.aipromptbackend.domain.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreadRepository extends JpaRepository<ThreadEntity, UUID> {

    @Query(
            """
                    SELECT t FROM ThreadEntity t
                    WHERE t.user = :user AND (:status is NULL OR t.status = :status)
                    AND (CAST(:updatedBefore AS java.time.Instant) IS NULL OR t.updatedAt < :updatedBefore)
                    ORDER BY t.updatedAt DESC
                    """
    )
    List<ThreadEntity> pageByUser(UserEntity user, String status, Instant updatedBefore, Pageable pageable);

    @Query("SELECT t.user.id FROM ThreadEntity t WHERE t.id = :threadId")
    Optional<UUID> findOwnerIdByThreadId(@Param("threadId") UUID threadId);}
