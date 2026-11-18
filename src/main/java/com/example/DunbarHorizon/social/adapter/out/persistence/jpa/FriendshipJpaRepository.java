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

public interface FriendshipJpaRepository extends JpaRepository<Friendship, FriendshipId> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.lowUserId = :userId AND f.highUserId IN :friendIds)
               OR (f.highUserId = :userId AND f.lowUserId IN :friendIds)
            ORDER BY f.lowUserId, f.highUserId
            """)
    List<Friendship> findAllBetweenForShare(@Param("userId") Long userId,
                                           @Param("friendIds") Collection<Long> friendIds);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.lowUserId = :low AND f.highUserId = :high")
    int deleteBetween(@Param("low") long low, @Param("high") long high);
}
