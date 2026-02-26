package com.example.GooRoomBe.social.application.port.in;

import com.example.GooRoomBe.social.application.port.in.dto.FriendshipUpdateCommand;

public interface FriendshipCommandUseCase {
    void updateFriendship(Long currentUserId, Long friendId, FriendshipUpdateCommand dto);
    void brokeUpWith(Long currentUserId, Long friendId);
}