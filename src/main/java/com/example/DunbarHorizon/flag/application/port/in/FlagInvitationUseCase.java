package com.example.DunbarHorizon.flag.application.port.in;

public interface FlagInvitationUseCase {
    void updateInvitePermission(Long flagId, Long requesterId, Long participantUserId, boolean canInvite);
    Long invite(Long flagId, Long inviterId, Long inviteeId);
    void accept(Long invitationId, Long acceptorId);
    void reject(Long invitationId, Long rejectorId);
}
