package com.example.DunbarHorizon.flag.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlagJpaRepository extends JpaRepository<Flag, Long> {

    Optional<Flag> findByParentId(Long parentId);

    boolean existsByParentId(Long parentId);

    List<Flag> findAllByIdIn(Collection<Long> ids);

    List<Flag> findAllByHostId(Long hostId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Flag f WHERE f.id = :id")
    Optional<Flag> findByIdExclusive(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Flag f " +
            "WHERE f.schedule.endDateTime < :threshold " +
            "AND NOT EXISTS (SELECT 1 FROM FlagMemorial m WHERE m.flagId = f.id)")
    int expireAllExceedingThreshold(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT f FROM Flag f WHERE f.hostId IN :hostIds AND f.schedule.deadline > :now")
    List<Flag> findRecruitingByHostIds(@Param("hostIds") Collection<Long> hostIds, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM Flag f WHERE f.hostId IN :hostIds AND f.schedule.deadline <= :now AND f.schedule.startDateTime > :now")
    List<Flag> findBeforeActivityByHostIds(@Param("hostIds") Collection<Long> hostIds, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM Flag f WHERE f.hostId IN :hostIds AND f.schedule.startDateTime <= :now AND f.schedule.endDateTime > :now")
    List<Flag> findInProgressByHostIds(@Param("hostIds") Collection<Long> hostIds, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM Flag f WHERE f.hostId IN :hostIds AND f.schedule.endDateTime <= :now")
    List<Flag> findEndedByHostIds(@Param("hostIds") Collection<Long> hostIds, @Param("now") LocalDateTime now);

    @Query("SELECT f.id FROM Flag f WHERE f.deletedAt < :bufferTime")
    List<Long> _findIdsInternal(@Param("bufferTime") LocalDateTime bufferTime, Pageable pageable);

    default List<Long> findIdsByDeletedAtBefore(LocalDateTime bufferTime) {
        return _findIdsInternal(bufferTime, PageRequest.of(0, 5000));
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Flag f WHERE f.id IN :ids")
    void hardDeleteByIdsIn(@Param("ids") Collection<Long> ids);
}