package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.*;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.SOCIAL_USER;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public interface FriendshipNeo4jRepository extends Neo4jRepository<Friendship, String> {

    default boolean existsFriendshipBetween(Long requesterId, Long receiverId) {
        return existsById(Friendship.generateCompositeId(requesterId, receiverId));
    }

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ") " +
            "MATCH (f)<-[all_r:" + HAS_FRIENDSHIP + "]-(all_u:" + USER_REFERENCE + ") " +
            "RETURN f, collect(all_r), collect(all_u)")
    List<Friendship> findByUserId(@Param("userId") Long userId);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->" +
            "(:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "RETURN friend.id")
    Set<Long> findFriendIdsByUserId(@Param("userId") Long userId);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->" +
            "(:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + SOCIAL_USER + ") " +
            "RETURN friend")
    List<SocialUser> findFriendsByUserId(@Param("userId") Long userId);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[r:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ") " +
            "WHERE r.isMuted = $isMuted " +
            "MATCH (f)<-[all_r:" + HAS_FRIENDSHIP + "]-(all_u:" + USER_REFERENCE + ") " +
            "RETURN f, collect(all_r), collect(all_u)")
    List<Friendship> findByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[r:" + HAS_FRIENDSHIP + "]->(:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE r.isMuted = $isMuted " +
            "RETURN friend.id")
    Set<Long> findFriendIdsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[r:" + HAS_FRIENDSHIP + "]->(:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + SOCIAL_USER + ") " +
            "WHERE r.isMuted = $isMuted " +
            "RETURN friend")
    List<SocialUser> findFriendsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE friend.id IN $candidateIds " +
            "MATCH (f)<-[all_r:" + HAS_FRIENDSHIP + "]-(all_u:" + USER_REFERENCE + ") " +
            "RETURN f, collect(all_r), collect(all_u)")
    List<Friendship> filterAmong(@Param("userId") Long userId, @Param("candidateIds") Collection<Long> candidateIds);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->" +
            "(:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE friend.id IN $candidateIds " +
            "RETURN friend.id")
    Set<Long> filterFriendIdsAmong(@Param("userId") Long userId, @Param("candidateIds") Collection<Long> candidateIds);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->" +
            "(:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + SOCIAL_USER + ") " +
            "WHERE friend.id IN $candidateIds " +
            "RETURN friend")
    List<SocialUser> filterFriendsAmong(@Param("userId") Long userId, @Param("candidateIds") Collection<Long> candidateIds);

    @Query("MATCH (u:" + USER_REFERENCE + ")-[r:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ") " +
            "WHERE r.interestScore > $threshold " +
            "AND r.lastInteractedAt <= $decayTime " +
            "SET r.interestScore = CASE " +
            "WHEN r.interestScore * $rate < $threshold THEN $threshold " +
            "ELSE r.interestScore * $rate " +
            "END " +
            "WITH DISTINCT f " +
            "MATCH (:" + USER_REFERENCE + ")-[all_r:" + HAS_FRIENDSHIP + "]->(f) " +
            "WITH f, collect(all_r.interestScore) as scores " +
            "SET f.intimacy = sqrt((scores[0] / (scores[0] + 50.0)) * (scores[1] / (scores[1] + 50.0)))")
    void applyDecay(@Param("rate") double rate, @Param("threshold") double threshold, @Param("decayTime") LocalDateTime decayTime);

    @Query("UNWIND $updates AS u " +
            "MATCH (:" + USER_REFERENCE + " {id: u.userId})-[r:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + " {id: u.friendshipId}) " +
            "SET r.interestScore = u.interestScore, r.lastInteractedAt = $lastInteractedAt, f.intimacy = u.intimacy")
    void batchUpdateInterestScores(@Param("updates") List<Map<String, Object>> updates,
                                   @Param("lastInteractedAt") LocalDateTime lastInteractedAt);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[r:" + HAS_FRIENDSHIP + "]->(:" + FRIENDSHIP + " {id: $friendshipId}) " +
            "SET r.friendAlias = $alias, r.isMuted = $isMuted, r.isRoutable = $isRoutable")
    void updateUserRelationshipFields(@Param("friendshipId") String friendshipId,
                                      @Param("userId") Long userId,
                                      @Param("alias") String alias,
                                      @Param("isMuted") boolean isMuted,
                                      @Param("isRoutable") boolean isRoutable);
}
