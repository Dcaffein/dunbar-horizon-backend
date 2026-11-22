package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuzzJpaRepository extends JpaRepository<Buzz, Long> {

    Optional<Buzz> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT b FROM Buzz b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Buzz> findByIdAndDeletedAtIsNullForShare(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Buzz b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Buzz> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);

    @Query("""
            SELECT new com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzDetailRow(
                b.id, b.creatorId, b.text, b.imageKeys, b.editedAt, b.expiresAt,
                t.id, t.starterId, t.visibility, t.createdAt)
            FROM Buzz b LEFT JOIN BuzzThread t ON t.buzzId = b.id
            WHERE b.id = :id
              AND b.expiresAt > :now
              AND b.deletedAt IS NULL
            ORDER BY t.createdAt ASC, t.id ASC
            """)
    List<BuzzDetailRow> findWithThreadsByIdAndExpiresAtAfterAndDeletedAtIsNull(
            @Param("id") Long id,
            @Param("now") LocalDateTime now);

    @Query("SELECT b.id FROM Buzz b WHERE b.expiresAt <= :now")
    List<Long> findIdsByExpiresAtAtOrBefore(@Param("now") LocalDateTime now);

    @Query("SELECT b.id FROM Buzz b WHERE b.deletedAt IS NOT NULL")
    List<Long> findIdsByDeletedAtIsNotNull();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Buzz b WHERE b.id IN :ids")
    int deleteAllByIdIn(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT new com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz(b, m.isRead)
            FROM Buzz b JOIN BuzzAudienceMember m ON m.buzzId = b.id
            WHERE m.userId = :userId
              AND b.expiresAt > :now
              AND b.deletedAt IS NULL
              AND b.creatorId NOT IN :excludedCreatorIds
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    Slice<ReceivedBuzz> findAllWithReadByMemberUserIdAndExpiresAtAfterAndDeletedAtIsNullAndCreatorIdNotIn(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("excludedCreatorIds") Collection<Long> excludedCreatorIds,
            Pageable pageable);

    @Query("""
            SELECT b FROM Buzz b
            WHERE b.creatorId = :creatorId
              AND b.expiresAt > :now
              AND b.deletedAt IS NULL
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    Slice<Buzz> findAllByCreatorIdAndExpiresAtAfterAndDeletedAtIsNull(
            @Param("creatorId") Long creatorId,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT b.creatorId
            FROM Buzz b JOIN BuzzAudienceMember m ON m.buzzId = b.id
            WHERE m.userId = :userId
              AND m.isRead = false
              AND b.expiresAt > :now
              AND b.deletedAt IS NULL
              AND b.creatorId NOT IN :excludedCreatorIds
            """)
    List<Long> findCreatorIdsByMemberUserIdAndMemberIsReadFalseAndExpiresAtAfterAndDeletedAtIsNullAndCreatorIdNotIn(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("excludedCreatorIds") Collection<Long> excludedCreatorIds);
}
