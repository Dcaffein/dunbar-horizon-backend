package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;

import java.util.List;

public interface FriendRequestUseCase {
    List<FriendRequestResult> getRequests(Long currentUserId, FriendRequestDirection direction, FriendRequestStatus status);
    FriendRequestResult sendRequest(Long currentUserId, Long counterpartId);
    void cancelRequest(Long currentUserId, Long counterpartId);
    void updateStatus(Long currentUserId, Long counterpartId, FriendRequestStatus status);
}
