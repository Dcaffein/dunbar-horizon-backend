package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;
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
                                           SocialUserProfile requester, SocialUserProfile receiver) {
        return new FriendRequestResult(
                Math.min(friendRequest.getRequesterId(), friendRequest.getReceiverId()) + "_"
                        + Math.max(friendRequest.getRequesterId(), friendRequest.getReceiverId()),
                requester != null ? FriendResult.from(requester) : null,
                receiver != null ? FriendResult.from(receiver) : null,
                friendRequest.getStatusFor(viewerId),
                friendRequest.getCreatedAt()
        );
    }
}
