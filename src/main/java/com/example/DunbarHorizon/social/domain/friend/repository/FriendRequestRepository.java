package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository{
    FriendRequest saveRequest(FriendRequest friendRequest);
    boolean existsRequestBetween(@Param("requesterId") Long requesterId, @Param("receiverId") Long receiverId);
    Optional<FriendRequest> findById(String requestId);
    List<FriendRequest> findAllByReceiver_IdAndStatus(Long receiverId, FriendRequestStatus status);
}
