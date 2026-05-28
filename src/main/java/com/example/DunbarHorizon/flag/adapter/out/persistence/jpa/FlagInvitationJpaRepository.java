package com.example.DunbarHorizon.flag.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface FlagInvitationJpaRepository extends JpaRepository<FlagInvitation, Long> {

    boolean existsByFlagIdAndInviteeIdAndStatus(Long flagId, Long inviteeId, FlagInvitationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FlagInvitation fi WHERE fi.flagId IN :flagIds")
    void hardDeleteByFlagIdsIn(@Param("flagIds") Collection<Long> flagIds);
}
