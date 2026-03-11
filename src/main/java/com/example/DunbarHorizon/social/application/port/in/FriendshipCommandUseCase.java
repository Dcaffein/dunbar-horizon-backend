package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;

public interface FriendshipCommandUseCase {
    void updateFriendship(Long currentUserId, Long friendId, FriendshipUpdateCommand dto);
    void brokeUpWith(Long currentUserId, Long friendId);
}