package com.example.DunbarHorizon.flag.domain.flag.event;

public record FlagInvitationSentEvent(
        Long flagId,
        Long invitationId,
        Long inviteeId,
        String flagTitle
) {}
