package com.example.DunbarHorizon.social.application.dto.command;

import lombok.Builder;

@Builder
public record FriendshipUpdateCommand(
        Long currentUserId,
        Long friendId,
        String friendAlias,
        Boolean isMuted,
        Boolean isRoutable
) {
}
