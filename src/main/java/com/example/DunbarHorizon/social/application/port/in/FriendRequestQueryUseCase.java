package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;

import java.util.List;

public interface FriendRequestQueryUseCase {
    List<FriendRequestResult> getReceivedRequests(Long userId);
}
