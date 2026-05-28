package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;

import java.time.LocalDateTime;

public record FriendRequestResult(
        String id,
        FriendResult requester,
        FriendResult receiver,
        FriendRequestStatus status,
        LocalDateTime createdAt
) {
    public static FriendRequestResult from(FriendRequest friendRequest) {
        return new FriendRequestResult(
                friendRequest.getId(),
                friendRequest.getRequester() != null ? FriendResult.from(friendRequest.getRequester()) : null,
                friendRequest.getReceiver() != null ? FriendResult.from(friendRequest.getReceiver()) : null,
                friendRequest.getStatus(),
                friendRequest.getCreatedAt()
        );
    }
}
