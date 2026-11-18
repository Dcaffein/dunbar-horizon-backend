package com.example.DunbarHorizon.social.application.friendrequest.dto;

import com.example.DunbarHorizon.social.domain.friendship.FriendRequest;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequestStatus;

import java.time.LocalDateTime;

public record FriendRequestResult(
        String id,
        FriendResult requester,
        FriendResult receiver,
        FriendRequestStatus status,
        LocalDateTime createdAt
) {
    public static FriendRequestResult fromSentRequest(FriendRequest request, FriendRequestView view) {
        return new FriendRequestResult(
                pairId(request.getRequesterId(), request.getReceiverId()),
                view.requesterNickname() != null ? new FriendResult(view.requesterId(), view.requesterNickname()) : null,
                view.receiverNickname() != null ? new FriendResult(view.receiverId(), view.receiverNickname()) : null,
                request.getStatusFor(request.getRequesterId()),
                request.getCreatedAt()
        );
    }

    public static FriendRequestResult from(FriendRequestView request, Long viewerId) {
        return new FriendRequestResult(
                pairId(request.requesterId(), request.receiverId()),
                new FriendResult(request.requesterId(), request.requesterNickname()),
                new FriendResult(request.receiverId(), request.receiverNickname()),
                request.status().visibleTo(request.requesterId(), viewerId),
                request.createdAt()
        );
    }

    private static String pairId(Long requesterId, Long receiverId) {
        return Math.min(requesterId, receiverId) + "_" + Math.max(requesterId, receiverId);
    }
}
