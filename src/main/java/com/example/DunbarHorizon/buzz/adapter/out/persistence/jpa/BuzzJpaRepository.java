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

public interface BuzzJpaRepository extends JpaRepository<Buzz, String> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT b FROM Buzz b WHERE b.id = :id")
    Optional<Buzz> findByIdForShare(@Param("id") String id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Buzz b WHERE b.id = :id")
    void deleteById(@Param("id") String id);

    @Query("""
            SELECT new com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz(b, m.isRead)
            FROM Buzz b JOIN BuzzAudienceMember m ON m.buzzId = b.id
            WHERE m.userId = :userId
              AND b.expiresAt > :now
              AND b.creatorId NOT IN :excludedCreatorIds
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    Slice<ReceivedBuzz> findAllWithReadByMemberUserIdAndExpiresAtAfterAndCreatorIdNotIn(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("excludedCreatorIds") Collection<Long> excludedCreatorIds,
            Pageable pageable);

    @Query("""
            SELECT b FROM Buzz b
            WHERE b.creatorId = :creatorId
              AND b.expiresAt > :now
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    Slice<Buzz> findAllByCreatorIdAndExpiresAtAfter(
            @Param("creatorId") Long creatorId,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT b.creatorId
            FROM Buzz b JOIN BuzzAudienceMember m ON m.buzzId = b.id
            WHERE m.userId = :userId
              AND m.isRead = false
              AND b.expiresAt > :now
              AND b.creatorId NOT IN :excludedCreatorIds
            """)
    List<Long> findCreatorIdsByMemberUserIdAndMemberIsReadFalseAndExpiresAtAfterAndCreatorIdNotIn(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("excludedCreatorIds") Collection<Long> excludedCreatorIds);
}
