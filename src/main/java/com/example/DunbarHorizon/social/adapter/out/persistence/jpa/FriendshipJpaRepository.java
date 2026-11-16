package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.example.DunbarHorizon.social.adapter.out.persistence.FriendshipConditions.ACTIVE_PAIR;

public interface FriendshipJpaRepository extends JpaRepository<Friendship, FriendshipId> {

    @Query(value = """
            SELECT * FROM friendships
            WHERE low_user_id = :low AND high_user_id = :high AND\s""" + ACTIVE_PAIR, nativeQuery = true)
    Optional<Friendship> findBetween(@Param("low") long low, @Param("high") long high);

    @Query(value = """
            SELECT * FROM friendships
            WHERE low_user_id = :userId AND\s""" + ACTIVE_PAIR + """

            UNION ALL
            SELECT * FROM friendships
            WHERE high_user_id = :userId AND\s""" + ACTIVE_PAIR, nativeQuery = true)
    List<Friendship> findByUserId(@Param("userId") long userId);

    @Query(value = """
            SELECT COUNT(*) FROM friendships
            WHERE low_user_id = :low AND high_user_id = :high AND\s""" + ACTIVE_PAIR, nativeQuery = true)
    long countBetween(@Param("low") long low, @Param("high") long high);

    @Modifying
    @Query(value = """
            DELETE FROM friendships
            WHERE low_user_id = :low AND high_user_id = :high AND\s""" + ACTIVE_PAIR, nativeQuery = true)
    int deleteBetween(@Param("low") long low, @Param("high") long high);
}
