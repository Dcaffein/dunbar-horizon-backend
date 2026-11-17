package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.dto.FriendRequestView;
import com.example.DunbarHorizon.social.application.dto.UserProfile;
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
    public static FriendRequestResult from(FriendRequest friendRequest, Long viewerId,
                                           UserProfile requester, UserProfile receiver) {
        return new FriendRequestResult(
                pairId(friendRequest.getRequesterId(), friendRequest.getReceiverId()),
                requester != null ? FriendResult.from(requester) : null,
                receiver != null ? FriendResult.from(receiver) : null,
                friendRequest.getStatusFor(viewerId),
                friendRequest.getCreatedAt()
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
