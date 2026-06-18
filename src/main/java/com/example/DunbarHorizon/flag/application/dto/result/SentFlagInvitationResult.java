package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;

import java.time.LocalDateTime;

public record SentFlagInvitationResult(
        Long id,
        Long flagId,
        String flagTitle,
        String flagDescription,
        String inviteeNickname,
        LocalDateTime createdAt
) {
    public static SentFlagInvitationResult of(FlagInvitation invitation, Flag flag, FlagUserInfo invitee) {
        return new SentFlagInvitationResult(
                invitation.getId(),
                invitation.getFlagId(),
                flag.getTitle(),
                flag.getDescription(),
                invitee.nickname(),
                invitation.getCreatedAt()
        );
    }
}
