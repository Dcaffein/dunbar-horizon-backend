package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData;

import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.schema.SocialGraphSchema.FRIENDS_WITH;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public interface FriendshipNeo4jRepository extends Repository<SocialUser, Long> {

    String LOCK_FRIENDSHIP =
            "SET r._interactionScoreLock = true " +
            "REMOVE r._interactionScoreLock ";

    String APPLY_MUTUAL_DELTA =
            "WITH r, delta, " +
            "coalesce(r.lowInterestScore, 0.0) + delta AS rawLow, " +
            "coalesce(r.highInterestScore, 0.0) + delta AS rawHigh " +
            "WITH r, " +
            "CASE WHEN rawLow < 0.0 THEN 0.0 ELSE rawLow END AS scoreLow, " +
            "CASE WHEN rawHigh < 0.0 THEN 0.0 ELSE rawHigh END AS scoreHigh " +
            "SET r.lowInterestScore = scoreLow, " +
            "r.lowLastInteractedAt = localdatetime(), " +
            "r.highInterestScore = scoreHigh, " +
            "r.highLastInteractedAt = localdatetime(), " +
            "r.intimacy = sqrt((scoreLow / (scoreLow + 50.0)) * (scoreHigh / (scoreHigh + 50.0)))";

    @Query("MATCH ()-[r:" + FRIENDS_WITH + " {id: $friendshipId}]->() RETURN count(r) > 0")
    boolean existsFriendship(@Param("friendshipId") String friendshipId);

    @Query("MATCH ()-[r:" + FRIENDS_WITH + " {id: $friendshipId}]->() DELETE r")
    void deleteFriendship(@Param("friendshipId") String friendshipId);

    @Query("UNWIND $friendshipIds AS friendshipId " +
            "WITH friendshipId ORDER BY friendshipId " +
            "MATCH ()-[r:" + FRIENDS_WITH + " {id: friendshipId}]->() " +
            "DELETE r")
    void deleteFriendships(@Param("friendshipIds") Collection<String> friendshipIds);

    @Query("MATCH (me:" + USER_REFERENCE + " {id: $userId})-[r:" + FRIENDS_WITH + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE CASE WHEN startNode(r) = me THEN r.lowIsMuted ELSE r.highIsMuted END = $isMuted " +
            "RETURN friend.id")
    Set<Long> findFriendIdsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (:" + USER_REFERENCE + " {id: $userId})-[:" + FRIENDS_WITH + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE friend.id IN $candidateIds " +
            "RETURN friend.id")
    Set<Long> filterFriendIdsAmong(@Param("userId") Long userId, @Param("candidateIds") Collection<Long> candidateIds);

    @Query("MATCH (:" + USER_REFERENCE + ")-[r:" + FRIENDS_WITH + "]->(:" + USER_REFERENCE + ") " +
            "WHERE r.lowLastInteractedAt <= $decayTime OR r.highLastInteractedAt <= $decayTime " +
            "WITH r ORDER BY r.id " +
            LOCK_FRIENDSHIP +
            "WITH r, " +
            "coalesce(r.lowInterestScore, 0.0) AS low, " +
            "coalesce(r.highInterestScore, 0.0) AS high " +
            "WITH r, " +
            "CASE WHEN low > $threshold AND r.lowLastInteractedAt <= $decayTime " +
            "THEN CASE WHEN low * $rate < $threshold THEN $threshold ELSE low * $rate END " +
            "ELSE low END AS scoreLow, " +
            "CASE WHEN high > $threshold AND r.highLastInteractedAt <= $decayTime " +
            "THEN CASE WHEN high * $rate < $threshold THEN $threshold ELSE high * $rate END " +
            "ELSE high END AS scoreHigh " +
            "SET r.lowInterestScore = scoreLow, " +
            "r.highInterestScore = scoreHigh, " +
            "r.intimacy = sqrt((scoreLow / (scoreLow + 50.0)) * (scoreHigh / (scoreHigh + 50.0)))")
    void applyDecay(@Param("rate") double rate, @Param("threshold") double threshold, @Param("decayTime") LocalDateTime decayTime);

    @Query("MATCH (:" + USER_REFERENCE + ")-[r:" + FRIENDS_WITH + " {id: $friendshipId}]->(:" + USER_REFERENCE + ") " +
            "SET r += $properties")
    void updateUserRelationshipFields(@Param("friendshipId") String friendshipId,
                                      @Param("properties") Map<String, Object> properties);

    // 아래 세 쿼리가 저장 점수를 0에서 끊는다(FriendRecognition.adjustInterestScore와 같은 동작).
    // 그래서 읽기만 하는 쪽은 따로 막지 않는다.
    @Query("MATCH (low:" + USER_REFERENCE + ")-[r:" + FRIENDS_WITH + " {id: $friendshipId}]->(:" + USER_REFERENCE + ") " +
            LOCK_FRIENDSHIP +
            "WITH r, low.id = $userId AS isLow " +
            "WITH r, isLow, " +
            "coalesce(r.lowInterestScore, 0.0) + CASE WHEN isLow THEN $delta ELSE 0.0 END AS rawLow, " +
            "coalesce(r.highInterestScore, 0.0) + CASE WHEN isLow THEN 0.0 ELSE $delta END AS rawHigh " +
            "WITH r, isLow, " +
            "CASE WHEN rawLow < 0.0 THEN 0.0 ELSE rawLow END AS scoreLow, " +
            "CASE WHEN rawHigh < 0.0 THEN 0.0 ELSE rawHigh END AS scoreHigh " +
            "SET r.lowInterestScore = scoreLow, " +
            "r.highInterestScore = scoreHigh, " +
            "r.lowLastInteractedAt = CASE WHEN isLow THEN localdatetime() ELSE r.lowLastInteractedAt END, " +
            "r.highLastInteractedAt = CASE WHEN isLow THEN r.highLastInteractedAt ELSE localdatetime() END, " +
            "r.intimacy = sqrt((scoreLow / (scoreLow + 50.0)) * (scoreHigh / (scoreHigh + 50.0)))")
    void incrementInterestScore(@Param("friendshipId") String friendshipId,
                                @Param("userId") Long userId,
                                @Param("delta") double delta);

    @Query("MATCH (:" + USER_REFERENCE + ")-[r:" + FRIENDS_WITH + " {id: $friendshipId}]->(:" + USER_REFERENCE + ") " +
            LOCK_FRIENDSHIP +
            "WITH r, $delta AS delta " +
            APPLY_MUTUAL_DELTA)
    void incrementMutualInterestScore(@Param("friendshipId") String friendshipId, @Param("delta") double delta);

    @Query("UNWIND $updates AS update " +
            "WITH update ORDER BY update.friendshipId " +
            "MATCH (:" + USER_REFERENCE + ")-[r:" + FRIENDS_WITH + " {id: update.friendshipId}]->(:" + USER_REFERENCE + ") " +
            LOCK_FRIENDSHIP +
            "WITH r, update.delta AS delta " +
            APPLY_MUTUAL_DELTA)
    void incrementMutualInterestScoresBatch(@Param("updates") List<Map<String, Object>> updates);
}
