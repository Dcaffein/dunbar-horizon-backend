package com.example.DunbarHorizon.flag.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FlagInvitationJpaRepository extends JpaRepository<FlagInvitation, Long> {

    List<FlagInvitation> findAllByInviteeIdOrderByCreatedAtDesc(Long inviteeId);

    List<FlagInvitation> findAllByInviterIdOrderByCreatedAtDesc(Long inviterId);

    boolean existsByFlagIdAndInviteeIdAndStatus(Long flagId, Long inviteeId, FlagInvitationStatus status);

    @Query("SELECT fi.inviteeId FROM FlagInvitation fi WHERE fi.flagId = :flagId AND fi.status = 'PENDING'")
    Set<Long> findPendingInviteeIdsByFlagId(@Param("flagId") Long flagId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FlagInvitation fi WHERE fi.flagId IN :flagIds")
    void hardDeleteByFlagIdsIn(@Param("flagIds") Collection<Long> flagIds);
}
