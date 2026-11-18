package com.example.DunbarHorizon.social.adapter.out.persistence.friendrequest;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FriendRequestJpaRepository extends JpaRepository<FriendRequest, FriendRequestId> {

    String BETWEEN_ACTIVE_USERS = """
            WHERE :userId <> :counterpartId
              AND ((r.requesterId = :userId AND r.receiverId = :counterpartId)
                OR (r.requesterId = :counterpartId AND r.receiverId = :userId))
              AND EXISTS (SELECT u.id FROM ActiveUser u WHERE u.id = r.requesterId)
              AND EXISTS (SELECT u.id FROM ActiveUser u WHERE u.id = r.receiverId)
            """;

    @Query("SELECT r FROM FriendRequest r " + BETWEEN_ACTIVE_USERS)
    Optional<FriendRequest> findBetweenActiveUsers(@Param("userId") Long userId,
                                                   @Param("counterpartId") Long counterpartId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM FriendRequest r
            """ + BETWEEN_ACTIVE_USERS)
    boolean existsBetweenActiveUsers(@Param("userId") Long userId,
                                     @Param("counterpartId") Long counterpartId);

    @Query("""
            SELECT CASE
                WHEN NOT EXISTS (SELECT u.id FROM ActiveUser u WHERE u.id = :requesterId) THEN :requesterId
                WHEN NOT EXISTS (SELECT u.id FROM ActiveUser u WHERE u.id = :receiverId) THEN :receiverId
                ELSE NULL
            END
            """)
    Optional<Long> findUnavailableParticipant(@Param("requesterId") Long requesterId,
                                              @Param("receiverId") Long receiverId);
}
