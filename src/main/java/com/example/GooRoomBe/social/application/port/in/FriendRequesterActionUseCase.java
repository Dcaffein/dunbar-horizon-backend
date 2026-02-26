package com.example.GooRoomBe.social.application.port.in;

import com.example.GooRoomBe.social.domain.friend.FriendRequest;

public interface FriendRequesterActionUseCase {
    FriendRequest sendRequest(Long requesterId, Long receiverId);
    void cancelRequest(String requestId, Long requesterId);
}