package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;

import java.time.LocalDateTime;

public record ReceivedFlagInvitationResult(
        Long id,
        Long flagId,
        String flagTitle,
        String flagDescription,
        String inviterNickname,
        LocalDateTime createdAt
) {
    public static ReceivedFlagInvitationResult of(FlagInvitation invitation, Flag flag, FlagUserInfo inviter) {
        return new ReceivedFlagInvitationResult(
                invitation.getId(),
                invitation.getFlagId(),
                flag.getTitle(),
                flag.getDescription(),
                inviter.nickname(),
                invitation.getCreatedAt()
        );
    }
}
