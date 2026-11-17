package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipJpaRepository extends JpaRepository<Friendship, FriendshipId> {

    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.lowUserId = :low AND f.highUserId = :high")
    int deleteBetween(@Param("low") long low, @Param("high") long high);
}
