package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository {
    void create(FriendRequest request);
    Optional<FriendRequest> findBetween(Long userId, Long counterpartId);
    boolean existsBetween(Long userId, Long counterpartId);
    void delete(FriendRequest request);
    List<FriendRequest> findAllByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status);
    List<FriendRequest> findAllByRequesterIdAndStatus(Long requesterId, FriendRequestStatus status);
}
