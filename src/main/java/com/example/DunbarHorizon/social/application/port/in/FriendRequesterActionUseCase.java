package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;

public interface FriendRequesterActionUseCase {
    FriendRequestResult sendRequest(Long requesterId, Long receiverId);
    void cancelRequest(Long counterpartId, Long requesterId);
}
