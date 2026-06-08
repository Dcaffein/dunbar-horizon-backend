package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import lombok.NonNull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.*;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public interface FriendshipNeo4jRepository extends Neo4jRepository<Friendship, String> {

    @Override
    @NonNull
    Optional<Friendship> findById(@NonNull String id);

    default boolean existsFriendshipBetween(Long requesterId, Long receiverId) {
        return existsById(Friendship.generateCompositeId(requesterId, receiverId));
    }

    @Query("MATCH (me:UserReference {id: $userId})-[r1:HAS_FRIENDSHIP]->(f:Friendship)<-[r2:HAS_FRIENDSHIP]-(friend:UserReference) " +
            "RETURN f, collect(r1), collect(me), collect(r2), collect(friend)")
    List<Friendship> findFriendshipsByUserId(Long userId);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->" +
            "(:" + FRIENDSHIP + ")" +
            "<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE friend.id IN $potentialMemberIds " +
            "RETURN friend.id")
    Set<Long> findFriendIdsIn(@Param("userId") Long userId, @Param("potentialMemberIds") Collection<Long> potentialMemberIds);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->" +
            "(:" + FRIENDSHIP + ")" +
            "<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "RETURN friend.id")
    Set<Long> findFriendIds(@Param("userId") Long userId);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[r:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE r.isMuted = $isMuted " +
            "RETURN friend.id")
    Set<Long> findFriendIdsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[r:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ") " +
            "WHERE r.isMuted = $isMuted " +
            "MATCH (f)<-[all_r:" + HAS_FRIENDSHIP + "]-(all_u:" + USER_REFERENCE + ") " +
            "RETURN f, collect(all_r), collect(all_u)")
    List<Friendship> findFriendshipsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE friend.id IN $targetIds " +
            "MATCH (f)<-[all_r:" + HAS_FRIENDSHIP + "]-(all_u:" + USER_REFERENCE + ") " +
            "RETURN f, collect(all_r), collect(all_u)")
    List<Friendship> findFriendshipsIn(@Param("userId") Long userId, @Param("targetIds") Collection<Long> targetIds);

    @Query("MATCH (u:" + USER_REFERENCE + ")-[r:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ") " +
            "WHERE r.interestScore > $threshold " +
            "AND r.lastInteractedAt <= $decayTime " +
            "SET r.interestScore = CASE " +
            "WHEN r.interestScore * $rate < $threshold THEN $threshold " +
            "ELSE r.interestScore * $rate " +
            "END " +
            "WITH f, collect(r.interestScore) as scores " +
            "SET f.intimacy = sqrt(" +
            "CASE WHEN size(scores) >= 2 " +
            "THEN (scores[0] / (scores[0] + 50.0)) * (scores[1] / (scores[1] + 50.0)) " +
            "ELSE 0.0 END" +
            ")")
    void applyDecay(@Param("rate") double rate, @Param("threshold") double threshold, @Param("decayTime") LocalDateTime decayTime);

    @Query("MATCH (u:" + USER_REFERENCE + ")-[:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(v:" + USER_REFERENCE + ") " +
            "WHERE f.id IN $ids " +
            "DETACH DELETE f")
    void deleteAllByIdIn(@Param("ids") Collection<String> ids);

    @Query("MATCH (u:" + USER_REFERENCE + ")-[r:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ") " +
            "WHERE f.id IN $ids " +
            "RETURN f, collect(r), collect(u)")
    List<Friendship> findAllByIds(@Param("ids") Collection<String> ids);

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