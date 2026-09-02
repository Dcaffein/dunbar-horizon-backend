package com.example.DunbarHorizon.social.adapter.in.web.dto;

import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import jakarta.validation.constraints.Size;

public record FriendUpdateRequest(
        @Size(max = Friendship.ALIAS_MAX_LENGTH, message = Friendship.ALIAS_LENGTH_MESSAGE)
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
