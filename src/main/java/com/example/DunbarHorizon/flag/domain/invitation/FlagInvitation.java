package com.example.DunbarHorizon.flag.domain.invitation;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationAccessException;
import com.example.DunbarHorizon.global.common.BaseTimeEntity;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipantNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationDuplicateException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "flag_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlagInvitation extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flagId;
    private Long inviterId;
    private Long inviteeId;

    private FlagInvitation(Long flagId, Long inviterId, Long inviteeId) {
        this.flagId = flagId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
    }

    public static FlagInvitation create(Flag flag, Long inviterId, Long inviteeId,
                                        FlagParticipant inviter, boolean inviteeParticipating,
                                        boolean alreadyInvited) {
        Long flagId = flag.getId();
        if (!flag.isRecruiting()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_NOT_RECRUITING, ErrorContext.of("flagId", flagId));
        }
        if (flag.getHostId().equals(inviteeId)) {
            throw new FlagAuthorizationException(FlagErrorCode.FLAG_HOST_NOT_ELIGIBLE,
                    ErrorContext.of("flagId", flagId).and("userId", inviteeId));
        }
        if (!flag.getHostId().equals(inviterId)) {
            if (inviter == null) throw new FlagParticipantNotFoundException(inviterId);
            if (!inviter.isCanInvite()) {
                throw new FlagAuthorizationException(FlagErrorCode.FLAG_INVITE_NOT_ALLOWED,
                        ErrorContext.of("flagId", flagId).and("userId", inviterId));
            }
        }
        if (inviteeParticipating) throw new FlagParticipationDuplicateException(flagId, inviteeId);
        if (alreadyInvited) throw new FlagInvitationDuplicateException(flagId, inviteeId);
        return new FlagInvitation(flagId, inviterId, inviteeId);
    }

    public void accept(Long requesterId) {
        validateInvitee(requesterId);
    }

    public void delete(Long requesterId) {
        if (inviteeId.equals(requesterId)) {
            reject(requesterId);
            return;
        }
        cancel(requesterId);
    }

    private void reject(Long requesterId) {
        validateInvitee(requesterId);
    }

    private void cancel(Long requesterId) {
        validateInviter(requesterId);
    }

    private void validateInvitee(Long requesterId) {
        if (!inviteeId.equals(requesterId)) {
            throw new FlagInvitationAccessException(FlagErrorCode.FLAG_INVITATION_INVITEE_ONLY, ErrorContext.of("invitationId", id).and("userId", requesterId));
        }
    }

    private void validateInviter(Long requesterId) {
        if (!inviterId.equals(requesterId)) {
            throw new FlagInvitationAccessException(FlagErrorCode.FLAG_INVITATION_SENDER_ONLY, ErrorContext.of("invitationId", id).and("userId", requesterId));
        }
    }

}
