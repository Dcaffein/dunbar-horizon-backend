package com.example.DunbarHorizon.flag.domain.invitation.repository;

import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;

import java.util.Collection;
import java.util.Optional;

public interface FlagInvitationRepository {
    FlagInvitation save(FlagInvitation invitation);
    Optional<FlagInvitation> findById(Long id);
    boolean existsPendingByFlagIdAndInviteeId(Long flagId, Long inviteeId);
    void hardDeleteByFlagIdsIn(Collection<Long> flagIds);
}
