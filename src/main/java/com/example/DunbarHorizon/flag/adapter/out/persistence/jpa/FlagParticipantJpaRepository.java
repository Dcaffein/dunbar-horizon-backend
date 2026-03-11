package com.example.DunbarHorizon.flag.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlagParticipantJpaRepository extends JpaRepository<FlagParticipant, Long> {

    Optional<FlagParticipant> findByFlagIdAndParticipantId(Long flagId, Long participantId);

    boolean existsByFlagIdAndParticipantId(Long flagId, Long participantId);

    int countByFlagId(Long flagId);

    List<FlagParticipant> findAllByFlagId(Long flagId);

    @Query("SELECT fp.participantId FROM FlagParticipant fp WHERE fp.flagId = :flagId")
    List<Long> findAllParticipantIdsByFlagId(@Param("flagId") Long flagId);

    List<Long> findFlagIdByParticipantId(Long participantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FlagParticipant fp WHERE fp.flagId = :flagId")
    void deleteAllByFlagId(@Param("flagId") Long flagId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FlagParticipant fp WHERE fp.flagId IN :flagIds")
    void hardDeleteByFlagIdsIn(@Param("flagIds") Collection<Long> flagIds);
}