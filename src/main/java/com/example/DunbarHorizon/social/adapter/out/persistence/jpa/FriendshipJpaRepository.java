package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FriendshipJpaRepository extends JpaRepository<Friendship, FriendshipId> {

    String BETWEEN_ACTIVE_USERS = """
            WHERE f.lowUserId = :low AND f.highUserId = :high
              AND EXISTS (SELECT u.id FROM ActiveUser u WHERE u.id = f.lowUserId)
              AND EXISTS (SELECT u.id FROM ActiveUser u WHERE u.id = f.highUserId)
            """;

    @Query("SELECT f FROM Friendship f " + BETWEEN_ACTIVE_USERS)
    Optional<Friendship> findBetweenActiveUsers(@Param("low") long low, @Param("high") long high);

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM Friendship f
            """ + BETWEEN_ACTIVE_USERS)
    boolean existsBetweenActiveUsers(@Param("low") long low, @Param("high") long high);

    @Modifying
    @Query("DELETE FROM Friendship f " + BETWEEN_ACTIVE_USERS)
    int deleteBetweenActiveUsers(@Param("low") long low, @Param("high") long high);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.lowUserId = :userId AND f.highUserId IN :friendIds)
               OR (f.highUserId = :userId AND f.lowUserId IN :friendIds)
            ORDER BY f.lowUserId, f.highUserId
            """)
    List<Friendship> findAllBetweenForShare(@Param("userId") Long userId,
                                           @Param("friendIds") Collection<Long> friendIds);
}
