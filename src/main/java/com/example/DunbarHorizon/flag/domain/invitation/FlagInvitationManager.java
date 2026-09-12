package com.example.DunbarHorizon.flag.domain.invitation;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationManager;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipantNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationDuplicateException;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationNotFoundException;
import com.example.DunbarHorizon.flag.domain.invitation.repository.FlagInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagInvitationManager {

    private final FlagRepository flagRepository;
    private final FlagInvitationRepository invitationRepository;
    private final FlagParticipationManager flagParticipationManager;

    public FlagParticipant updateInvitePermission(Long flagId, Long requesterId, Long participantUserId, boolean canInvite) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        FlagParticipant participant = flagRepository
                .findParticipant(flagId, participantUserId)
                .orElseThrow(() -> new FlagParticipantNotFoundException(participantUserId));

        if (canInvite) {
            flag.grantInvitePermission(requesterId, participant);
        } else {
            flag.revokeInvitePermission(requesterId, participant);
        }

        return participant;
    }

    public FlagInvitation invite(Long flagId, Long inviterId, Long inviteeId) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));

        if (!flag.isRecruiting()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_NOT_RECRUITING, ErrorContext.of("flagId", flag.getId()));
        }

        if (flag.getHostId().equals(inviteeId)) {
            throw new FlagAuthorizationException(FlagErrorCode.FLAG_HOST_NOT_ELIGIBLE, ErrorContext.of("flagId", flag.getId()).and("userId", inviteeId));
        }

        if (!flag.getHostId().equals(inviterId)) {
            FlagParticipant inviter = flagRepository
                    .findParticipant(flagId, inviterId)
                    .orElseThrow(() -> new FlagParticipantNotFoundException(inviterId));
            if (!inviter.isCanInvite()) {
                throw new FlagAuthorizationException(FlagErrorCode.FLAG_INVITE_NOT_ALLOWED, ErrorContext.of("flagId", flag.getId()).and("userId", inviterId));
            }
        }

        if (flagRepository.isParticipating(flagId, inviteeId)) {
            throw new FlagParticipationDuplicateException(flagId, inviteeId);
        }

        if (invitationRepository.existsByFlagIdAndInviteeId(flagId, inviteeId)) {
            throw new FlagInvitationDuplicateException(flagId, inviteeId);
        }

        return FlagInvitation.create(flagId, inviterId, inviteeId);
    }

    public FlagParticipant accept(Long invitationId, Long requesterId) {
        FlagInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new FlagInvitationNotFoundException(invitationId));

        invitation.accept(requesterId);

        return flagParticipationManager.participateByInvitation(invitation.getFlagId(), requesterId);
    }

}
