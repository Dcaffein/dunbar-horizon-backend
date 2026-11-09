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

public interface FriendshipNeo4jRepository extends Repository<SocialUser, Long> {

    String LOCK_FRIENDSHIP = """
            SET r._interactionScoreLock = true
            REMOVE r._interactionScoreLock
            """;

    @Query("""
            MATCH ()-[r:FRIENDS_WITH {id: $friendshipId}]->()
            RETURN count(r) > 0
            """)
    boolean existsFriendship(@Param("friendshipId") String friendshipId);

    @Query("""
            MATCH ()-[r:FRIENDS_WITH {id: $friendshipId}]->()
            DELETE r
            """)
    void deleteFriendship(@Param("friendshipId") String friendshipId);

    @Query("""
            UNWIND $friendshipIds AS friendshipId
            WITH friendshipId ORDER BY friendshipId
            MATCH ()-[r:FRIENDS_WITH {id: friendshipId}]->()
            DELETE r
            """)
    void deleteFriendships(@Param("friendshipIds") Collection<String> friendshipIds);

    @Query("""
            MATCH (me:UserReference {id: $userId})-[r:FRIENDS_WITH]-(friend:UserReference)
            WHERE CASE WHEN startNode(r) = me THEN r.lowIsMuted ELSE r.highIsMuted END = $isMuted
            RETURN friend.id
            """)
    Set<Long> findFriendIdsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("""
            MATCH (:UserReference {id: $userId})-[:FRIENDS_WITH]-(friend:UserReference)
            WHERE friend.id IN $candidateIds
            RETURN friend.id
            """)
    Set<Long> filterFriendIdsAmong(@Param("userId") Long userId, @Param("candidateIds") Collection<Long> candidateIds);

    @Query("""
            MATCH (:UserReference)-[r:FRIENDS_WITH]->(:UserReference)
            WHERE r.lowLastInteractedAt <= $decayTime OR r.highLastInteractedAt <= $decayTime
            WITH r ORDER BY r.id
            """ + LOCK_FRIENDSHIP + """
            WITH r,
                 coalesce(r.lowInterestScore, 0.0) AS low,
                 coalesce(r.highInterestScore, 0.0) AS high
            WITH r,
                 CASE WHEN low > $threshold AND r.lowLastInteractedAt <= $decayTime
                      THEN CASE WHEN low * $rate < $threshold THEN $threshold ELSE low * $rate END
                      ELSE low END AS scoreLow,
                 CASE WHEN high > $threshold AND r.highLastInteractedAt <= $decayTime
                      THEN CASE WHEN high * $rate < $threshold THEN $threshold ELSE high * $rate END
                      ELSE high END AS scoreHigh
            SET r.lowInterestScore = scoreLow,
                r.highInterestScore = scoreHigh,
                r.intimacy = sqrt((scoreLow / (scoreLow + 50.0)) * (scoreHigh / (scoreHigh + 50.0)))
            """)
    void applyDecay(@Param("rate") double rate, @Param("threshold") double threshold, @Param("decayTime") LocalDateTime decayTime);

    @Query("""
            MATCH (:UserReference)-[r:FRIENDS_WITH {id: $friendshipId}]->(:UserReference)
            SET r += $properties
            """)
    void updateUserRelationshipFields(@Param("friendshipId") String friendshipId,
                                      @Param("properties") Map<String, Object> properties);

    @Query("""
            UNWIND $friendshipIds AS friendshipId
            WITH friendshipId ORDER BY friendshipId
            MATCH (:UserReference)-[r:FRIENDS_WITH {id: friendshipId}]->(:UserReference)
            """ + LOCK_FRIENDSHIP)
    void lockFriendships(@Param("friendshipIds") Collection<String> friendshipIds);

    @Query("""
            UNWIND $rows AS row
            MATCH (:UserReference)-[r:FRIENDS_WITH {id: row.friendshipId}]->(:UserReference)
            SET r += row.properties
            """)
    void saveInterestScores(@Param("rows") List<Map<String, Object>> rows);
}
