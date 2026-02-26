package com.example.GooRoomBe.social.adapter.out.neo4j.springData;

import com.example.GooRoomBe.social.domain.socialUser.UserReference;
import com.example.GooRoomBe.social.domain.friend.Friendship;
import lombok.NonNull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.example.GooRoomBe.social.domain.friend.constant.FriendConstants.*;
import static com.example.GooRoomBe.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public interface FriendshipNeo4jRepository extends Neo4jRepository<Friendship, String> {

    @Override
    @NonNull
    Optional<Friendship> findById(@NonNull String id);

    @Query("RETURN EXISTS(" +
            "(:" + USER_REFERENCE + " {id: $requesterId})-[:" + MEMBER_OF + "]->" +
            "(:" + FRIENDSHIP + ")" +
            "<-[:" + MEMBER_OF + "]-(:" + USER_REFERENCE + " {id: $receiverId})" +
            ")")
    boolean existsFriendshipBetween(@Param("requesterId") Long requesterId, @Param("receiverId") Long receiverId);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[:" + MEMBER_OF + "]->" +
            "(:" + FRIENDSHIP + ")" +
            "<-[:" + MEMBER_OF + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE friend.id IN $potentialMemberIds " +
            "RETURN friend.id")
    Set<Long> findFriendIdsIn(@Param("userId") Long userId, @Param("potentialMemberIds") Collection<Long> potentialMemberIds);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[:" + MEMBER_OF + "]->" +
            "(:" + FRIENDSHIP + ")" +
            "<-[:" + MEMBER_OF + "]-(friend:" + USER_REFERENCE + ") " +
            "WHERE friend.id IN $targetIds " +
            "RETURN friend")
    Set<UserReference> findFriendsIn(@Param("userId") Long userId, @Param("targetIds") Collection<Long> targetIds);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[:" + MEMBER_OF + "]->" +
            "(:" + FRIENDSHIP + ")" +
            "<-[:" + MEMBER_OF + "]-(friend:" + USER_REFERENCE + ") " +
            "RETURN friend.id")
    Set<Long> findFriendIds(@Param("userId") Long userId);

    @Query("MATCH (u:" + USER_REFERENCE + " {id: $userId})-[:" + MEMBER_OF + "]->" +
            "(:" + FRIENDSHIP + ")" +
            "<-[:" + MEMBER_OF + "]-(friend:" + USER_REFERENCE + ") " +
            "RETURN friend")
    Set<UserReference> findFriends(@Param("userId") Long userId);

    @Query("MATCH (u:UserReference {id: $userId})-[r:MEMBER_OF]->(f:FriendShip)<-[:MEMBER_OF]-(friend:UserReference) " +
            "WHERE r.isMuted = $isMuted " +
            "RETURN friend.id")
    Set<Long> findFriendIdsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (u:UserReference {id: $userId})-[r:MEMBER_OF]->(f:FriendShip)<-[:MEMBER_OF]-(friend:UserReference) " +
            "WHERE r.isMuted = $isMuted " +
            "RETURN friend")
    Set<UserReference> findFriendsByMuteStatus(@Param("userId") Long userId, @Param("isMuted") boolean isMuted);

    @Query("MATCH (u:" + USER_REFERENCE + ")-[r:" + MEMBER_OF + "]->(f:" + FRIENDSHIP + ") " +
            "WHERE r.interestScore > 0 " +
            "AND r.lastInteractedAt <= $decayTime " +
            "SET r.interestScore = CASE " +
            "WHEN r.interestScore * $rate < $threshold THEN 0.0 " +
            "ELSE r.interestScore * $rate " +
            "END " +
            "WITH f, collect(r.interestScore) as scores " +
            "SET f.intimacy = sqrt(" +
            "CASE WHEN size(scores) >= 2 THEN scores[0] * scores[1] ELSE 0.0 END" +
            ")")
    void applyDecay(@Param("rate") double rate, @Param("threshold") double threshold, @Param("decayTime") LocalDateTime decayTime);
}
