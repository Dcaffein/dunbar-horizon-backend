package com.example.DunbarHorizon.social.application.friendrequest.port;

import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendRequestView;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequestStatus;

import java.util.List;
import java.util.Optional;

public interface FriendRequestQueryRepository {
    List<FriendRequestView> findSent(Long requesterId);

    Optional<FriendRequestView> findSentTo(Long requesterId, Long receiverId);

    List<FriendRequestView> findReceived(Long receiverId, FriendRequestStatus status);
}
