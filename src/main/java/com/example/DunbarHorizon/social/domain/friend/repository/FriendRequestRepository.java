package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;

import java.util.Optional;

public interface FriendRequestRepository {
    void create(FriendRequest request);
    Optional<FriendRequest> findBetween(Long userId, Long counterpartId);
    boolean existsBetween(Long userId, Long counterpartId);
    void delete(FriendRequest request);
}
