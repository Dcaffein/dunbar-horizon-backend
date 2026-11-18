package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, FriendRequestId> {
}
