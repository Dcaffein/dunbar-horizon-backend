package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.*;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public interface FriendshipNeo4jRepository extends Neo4jRepository<Friendship, String> {

    default boolean existsFriendshipBetween(Long requesterId, Long receiverId) {
        return existsById(Friendship.generateCompositeId(requesterId, receiverId));
    }

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->(f:" + FRIENDSHIP + ")" +
            "<-[:" + HAS_FRIENDSHIP + "]-(:" + USER_REFERENCE + ") " +
            "MATCH (f)<-[all_r:" + HAS_FRIENDSHIP + "]-(all_u) " +
            "RETURN f, collect(all_r), collect(all_u)")
    List<Friendship> findByUserId(@Param("userId") Long userId);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[r:" + HAS_FRIENDSHIP + "]->(:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE r.isMuted = $isMuted " +
            "RETURN friend.id")
    Set<Long> findFriendIdsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + HAS_FRIENDSHIP + "]->" +
            "(:" + FRIENDSHIP + ")<-[:" + HAS_FRIENDSHIP + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE friend.id IN $candidateIds " +
            "RETURN friend.id")
    Set<Long> filterFriendIdsAmong(@Param("userId") Long userId, @Param("candidateIds") Collection<Long> candidateIds);

    @Transactional(transactionManager = "neo4jTransactionManager")
    @Query("MATCH (:" + USER_REFERENCE + ")-[candidate:" + HAS_FRIENDSHIP + "]->(friendship:" + FRIENDSHIP + ") " +
            "WHERE candidate.lastInteractedAt <= $decayTime " +
            "WITH DISTINCT friendship ORDER BY friendship.id " +
            "SET friendship._interactionScoreLock = true " +
            "REMOVE friendship._interactionScoreLock " +
            "WITH friendship " +
            "MATCH (:" + USER_REFERENCE + ")-[recognition:" + HAS_FRIENDSHIP + "]->(friendship) " +
            "WHERE recognition.interestScore > $threshold " +
            "AND recognition.lastInteractedAt <= $decayTime " +
            "SET recognition.interestScore = CASE " +
            "WHEN recognition.interestScore * $rate < $threshold THEN $threshold " +
            "ELSE recognition.interestScore * $rate " +
            "END " +
            "WITH DISTINCT friendship " +
            "MATCH (:" + USER_REFERENCE + ")-[allRecognition:" + HAS_FRIENDSHIP + "]->(friendship) " +
            "WITH friendship, collect(allRecognition.interestScore) AS scores " +
            "SET friendship.intimacy = sqrt((scores[0] / (scores[0] + 50.0)) * (scores[1] / (scores[1] + 50.0)))")
    void applyDecay(@Param("rate") double rate, @Param("threshold") double threshold, @Param("decayTime") LocalDateTime decayTime);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[r:" + HAS_FRIENDSHIP + "]->(:" + FRIENDSHIP + " {id: $friendshipId}) " +
            "SET r.friendAlias = $alias, r.isMuted = $isMuted, r.isRoutable = $isRoutable")
    void updateUserRelationshipFields(@Param("friendshipId") String friendshipId,
                                      @Param("userId") Long userId,
                                      @Param("alias") String alias,
                                      @Param("isMuted") boolean isMuted,
                                      @Param("isRoutable") boolean isRoutable);

    @Transactional(transactionManager = "neo4jTransactionManager")
    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[recognition:" + HAS_FRIENDSHIP + "]->" +
            "(friendship:" + FRIENDSHIP + " {id: $friendshipId})<-[otherRecognition:" + HAS_FRIENDSHIP + "]-" +
            "(:" + USER_REFERENCE + " {id: $friendId}) " +
            "SET friendship._interactionScoreLock = true " +
            "REMOVE friendship._interactionScoreLock " +
            "SET recognition.interestScore = coalesce(recognition.interestScore, 0.0) + $delta, " +
            "recognition.lastInteractedAt = localdatetime() " +
            "WITH friendship, recognition, otherRecognition " +
            "SET friendship.intimacy = sqrt(" +
            "(coalesce(recognition.interestScore, 0.0) / (coalesce(recognition.interestScore, 0.0) + 50.0)) * " +
            "(coalesce(otherRecognition.interestScore, 0.0) / (coalesce(otherRecognition.interestScore, 0.0) + 50.0))" +
            ")")
    void incrementInterestScore(@Param("friendshipId") String friendshipId,
                                @Param("userId") Long userId,
                                @Param("friendId") Long friendId,
                                @Param("delta") double delta);

    @Transactional(transactionManager = "neo4jTransactionManager")
    @Query("MATCH (:" + USER_REFERENCE + " {id: $userAId})-[recognitionA:" + HAS_FRIENDSHIP + "]->" +
            "(friendship:" + FRIENDSHIP + " {id: $friendshipId})<-[recognitionB:" + HAS_FRIENDSHIP + "]-" +
            "(:" + USER_REFERENCE + " {id: $userBId}) " +
            "SET friendship._interactionScoreLock = true " +
            "REMOVE friendship._interactionScoreLock " +
            "SET recognitionA.interestScore = coalesce(recognitionA.interestScore, 0.0) + $delta, " +
            "recognitionA.lastInteractedAt = localdatetime(), " +
            "recognitionB.interestScore = coalesce(recognitionB.interestScore, 0.0) + $delta, " +
            "recognitionB.lastInteractedAt = localdatetime() " +
            "WITH friendship, recognitionA, recognitionB " +
            "SET friendship.intimacy = sqrt(" +
            "(coalesce(recognitionA.interestScore, 0.0) / (coalesce(recognitionA.interestScore, 0.0) + 50.0)) * " +
            "(coalesce(recognitionB.interestScore, 0.0) / (coalesce(recognitionB.interestScore, 0.0) + 50.0))" +
            ")")
    void incrementMutualInterestScore(@Param("friendshipId") String friendshipId,
                                      @Param("userAId") Long userAId,
                                      @Param("userBId") Long userBId,
                                      @Param("delta") double delta);

    @Transactional(transactionManager = "neo4jTransactionManager")
    @Query("UNWIND $updates AS update " +
            "WITH update ORDER BY update.friendshipId " +
            "MATCH (:" + USER_REFERENCE + " {id: update.userAId})-[recognitionA:" + HAS_FRIENDSHIP + "]->" +
            "(friendship:" + FRIENDSHIP + " {id: update.friendshipId})<-[recognitionB:" + HAS_FRIENDSHIP + "]-" +
            "(:" + USER_REFERENCE + " {id: update.userBId}) " +
            "SET friendship._interactionScoreLock = true " +
            "REMOVE friendship._interactionScoreLock " +
            "SET recognitionA.interestScore = coalesce(recognitionA.interestScore, 0.0) + update.delta, " +
            "recognitionA.lastInteractedAt = localdatetime(), " +
            "recognitionB.interestScore = coalesce(recognitionB.interestScore, 0.0) + update.delta, " +
            "recognitionB.lastInteractedAt = localdatetime() " +
            "WITH friendship, recognitionA, recognitionB " +
            "SET friendship.intimacy = sqrt(" +
            "(coalesce(recognitionA.interestScore, 0.0) / (coalesce(recognitionA.interestScore, 0.0) + 50.0)) * " +
            "(coalesce(recognitionB.interestScore, 0.0) / (coalesce(recognitionB.interestScore, 0.0) + 50.0))" +
            ")")
    void incrementMutualInterestScoresBatch(@Param("updates") List<Map<String, Object>> updates);
}
