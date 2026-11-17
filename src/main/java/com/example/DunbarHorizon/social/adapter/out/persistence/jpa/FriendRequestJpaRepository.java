package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.example.DunbarHorizon.social.adapter.out.persistence.FriendRequestConditions.ACTIVE_PAIR;

public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, FriendRequestId> {

    @Query(value = """
            SELECT * FROM friend_requests
            WHERE ((requester_id = :userId AND receiver_id = :counterpartId)
                OR (requester_id = :counterpartId AND receiver_id = :userId))
              AND\s""" + ACTIVE_PAIR, nativeQuery = true)
    Optional<FriendRequest> findBetween(@Param("userId") long userId, @Param("counterpartId") long counterpartId);

    @Query(value = """
            SELECT COUNT(*) FROM friend_requests
            WHERE ((requester_id = :userId AND receiver_id = :counterpartId)
                OR (requester_id = :counterpartId AND receiver_id = :userId))
              AND\s""" + ACTIVE_PAIR, nativeQuery = true)
    long countBetween(@Param("userId") long userId, @Param("counterpartId") long counterpartId);

    @Query(value = """
            SELECT * FROM friend_requests
            WHERE receiver_id = :receiverId AND status = :status
              AND\s""" + ACTIVE_PAIR, nativeQuery = true)
    List<FriendRequest> findAllByReceiverIdAndStatus(@Param("receiverId") long receiverId, @Param("status") String status);

    @Query(value = """
            SELECT * FROM friend_requests
            WHERE requester_id = :requesterId
              AND\s""" + ACTIVE_PAIR, nativeQuery = true)
    List<FriendRequest> findAllByRequesterId(@Param("requesterId") long requesterId);
}
