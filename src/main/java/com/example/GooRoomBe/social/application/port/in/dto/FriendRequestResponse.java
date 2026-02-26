package com.example.GooRoomBe.social.application.port.in.dto;

import com.example.GooRoomBe.social.domain.friend.FriendRequest;
import com.example.GooRoomBe.social.domain.friend.FriendRequestStatus;

import java.time.LocalDateTime;

public record FriendRequestResponse(
        String id,
        FriendResponse requester,
        FriendResponse receiver,
        FriendRequestStatus status,
        LocalDateTime createdAt
) {
    public static FriendRequestResponse from(FriendRequest friendRequest) {
        return new FriendRequestResponse(
                friendRequest.getId(),
                FriendResponse.from(friendRequest.getRequester()),
                FriendResponse.from(friendRequest.getReceiver()),
                friendRequest.getStatus(),
                friendRequest.getCreatedAt()
        );
    }
}