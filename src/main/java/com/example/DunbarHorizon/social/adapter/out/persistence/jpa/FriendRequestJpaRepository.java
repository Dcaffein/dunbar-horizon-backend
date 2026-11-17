package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestId;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, FriendRequestId> {

    List<FriendRequest> findAllByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status);

    List<FriendRequest> findAllByRequesterId(Long requesterId);
}
