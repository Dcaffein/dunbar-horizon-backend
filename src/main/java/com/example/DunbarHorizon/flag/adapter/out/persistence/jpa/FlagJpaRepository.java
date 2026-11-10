package com.example.DunbarHorizon.flag.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlagJpaRepository extends JpaRepository<Flag, Long> {

    Optional<Flag> findByParentId(Long parentId);

    boolean existsByParentId(Long parentId);

    List<Flag> findAllByIdIn(Collection<Long> ids);

    Slice<Flag> findAllByHostId(Long hostId, Pageable pageable);

    @Query("SELECT f FROM Flag f " +
           "WHERE f.id IN (SELECT fp.flagId FROM FlagParticipant fp WHERE fp.participantId = :participantId)")
    Slice<Flag> findByParticipantId(@Param("participantId") Long participantId, Pageable pageable);

    @Transactional(readOnly = true)
    @Query("SELECT f.hostId FROM Flag f WHERE f.id = :id")
    Optional<Long> findHostIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Flag f WHERE f.id = :id")
    Optional<Flag> findByIdForUpdate(@Param("id") Long id);

    // @SQLRestriction이 deleted_at IS NULL을 붙이므로 이미 삭제된 행은 자동으로 빠진다.
    @Query("SELECT f FROM Flag f " +
            "WHERE f.schedule.endDateTime < :threshold " +
            "AND f.preserved = false " +
            "ORDER BY f.schedule.endDateTime ASC")
    List<Flag> findConclusionCandidates(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT f FROM Flag f WHERE f.hostId IN :hostIds AND f.schedule.deadline > :asOf")
    Slice<Flag> findByHostIdsAndDeadlineAfter(
            @Param("hostIds") Collection<Long> hostIds,
            @Param("asOf") LocalDateTime asOf,
            Pageable pageable
    );

    // Flag의 @SQLRestriction("deleted_at IS NULL")이 JPQL에 적용되어 찾으려는 소프트 삭제 행을
    // 정확히 걸러내므로 네이티브 쿼리로 우회한다.
    @Query(value = "SELECT id FROM flags WHERE deleted_at < :bufferTime LIMIT :batchSize",
           nativeQuery = true)
    List<Long> findIdsByDeletedAtBefore(@Param("bufferTime") LocalDateTime bufferTime,
                                        @Param("batchSize") int batchSize);

    @Query("SELECT f FROM Flag f " +
           "WHERE f.hostId = :userId " +
           "OR f.id IN (SELECT fp.flagId FROM FlagParticipant fp WHERE fp.participantId = :userId) " +
           "ORDER BY f.createdAt DESC")
    List<Flag> findByHostIdOrParticipantId(@Param("userId") Long userId, Pageable pageable);

    // @SQLRestriction은 벌크 DELETE에도 적용된다. JPQL로 쓰면 지우려는 소프트 삭제 행이
    // 정확히 걸러져 한 건도 지워지지 않으므로 네이티브 쿼리로 우회한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM flags WHERE id IN (:ids)", nativeQuery = true)
    void hardDeleteByIdsIn(@Param("ids") Collection<Long> ids);
}
