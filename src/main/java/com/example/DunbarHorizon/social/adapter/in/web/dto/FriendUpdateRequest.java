package com.example.DunbarHorizon.social.adapter.in.web.dto;

import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import jakarta.validation.constraints.Size;

public record FriendUpdateRequest(
        @Size(min = 1, max = 20, message = "별명은 1자 이상 20자 이내여야 합니다.")
        String friendAlias,
        Boolean isMuted,
        Boolean isRoutable
) {
        public FriendshipUpdateCommand toCommand(Long currentUserId, Long friendId) {
                return FriendshipUpdateCommand.builder()
                        .currentUserId(currentUserId)
                        .friendId(friendId)
                        .friendAlias(this.friendAlias)
                        .isMuted(isMuted)
                        .isRoutable(isRoutable)
                        .build();
        }
}
