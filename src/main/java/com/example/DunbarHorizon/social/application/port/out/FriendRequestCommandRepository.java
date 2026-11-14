package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;

import java.util.Optional;

public interface FriendRequestCommandRepository extends FriendRequestRepository {
    Optional<FriendRequest> findByIdForUpdate(String requestId);
}
