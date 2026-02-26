package com.example.GooRoomBe.social.application.port.in.dto;

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