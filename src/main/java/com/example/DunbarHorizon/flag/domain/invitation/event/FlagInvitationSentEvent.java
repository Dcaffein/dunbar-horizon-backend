package com.example.DunbarHorizon.flag.domain.invitation.event;

public record FlagInvitationSentEvent(
        Long flagId,
        Long invitationId,
        Long inviteeId,
        String flagTitle,
        boolean isEncore
) {}
