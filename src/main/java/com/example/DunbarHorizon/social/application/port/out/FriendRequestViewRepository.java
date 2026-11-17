package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.FriendRequestView;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;

import java.util.List;

public interface FriendRequestViewRepository {
    List<FriendRequestView> findSent(Long requesterId);

    List<FriendRequestView> findReceived(Long receiverId, FriendRequestStatus status);
}
