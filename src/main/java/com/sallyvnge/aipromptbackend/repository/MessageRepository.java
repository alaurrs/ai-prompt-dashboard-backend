package com.sallyvnge.aipromptbackend.repository;

import com.sallyvnge.aipromptbackend.domain.MessageEntity;
import com.sallyvnge.aipromptbackend.domain.ThreadEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    List<MessageEntity> findByThreadAndPositionGreaterThanOrderByPositionAsc(ThreadEntity thread, int afterPosition, Pageable pageable);
    Optional<MessageEntity> findTopByThreadOrderByPositionDesc(ThreadEntity thread);

    @Modifying
    @Query(value = """
    update messages
       set content_delta = coalesce(content_delta,'') || :delta,
           updated_at = now()
     where id = :messageId
  """, nativeQuery = true)
    int appendDelta(@Param("messageId") UUID messageId, @Param("delta") String delta);

    @Modifying
    @Query(value = """
    update messages
       set status = 'error',
           error_code = :code,
           error_message = :msg,
           updated_at = now()
     where id = :messageId
  """, nativeQuery = true)
    int markError(@Param("messageId") UUID messageId,
                  @Param("code") String code,
                  @Param("msg") String msg);
}
