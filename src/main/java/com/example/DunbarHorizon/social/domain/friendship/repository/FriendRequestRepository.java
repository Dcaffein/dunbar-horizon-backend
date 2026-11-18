package com.example.DunbarHorizon.social.domain.friendship.repository;

import com.example.DunbarHorizon.social.domain.friendship.FriendRequest;

import java.util.Optional;

public interface FriendRequestRepository {
    void create(FriendRequest request);
    Optional<FriendRequest> findBetween(Long userId, Long counterpartId);
    boolean existsBetween(Long userId, Long counterpartId);
    void saveStatus(FriendRequest request);
    void delete(FriendRequest request);
}
