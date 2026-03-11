package com.example.DunbarHorizon.social.adapter.out.neo4j.springData;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import lombok.NonNull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.*;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public interface FriendRequestNeo4jRepository extends Neo4jRepository<FriendRequest, String> {

    @NonNull
    @Override
    Optional<FriendRequest> findById(@NonNull String id);

    @Query("MATCH (req: " + USER_REFERENCE + " {id: $requesterId}), (rec:" + USER_REFERENCE + " {id: $receiverId}) " +
            "MERGE (req)-[:" + SENT_FRIEND_REQUEST + "]->(fr:" + FRIEND_REQUEST + ")-[:" + FRIEND_REQUEST_TO + "]->(rec) " +
            "ON CREATE SET " +
            "  fr.id = $requestId, " +
            "  fr.status = 'PENDING', " +
            "  fr.createdAt = localdatetime() " +
            "RETURN fr")
    FriendRequest mergeFriendRequest(
            @Param("requesterId") Long requesterId,
            @Param("receiverId") Long receiverId,
            @Param("requestId") String requestId
    );

    @Query("MATCH (n:" + FRIEND_REQUEST + " {id: :#{#entity.id}}) SET n.status = :#{#entity.status} RETURN n")
    FriendRequest updateFriendRequest(@Param("entity") FriendRequest entity);

    @Query("RETURN EXISTS(" +
            "(:" + USER_REFERENCE + " {id: $requesterId})" +
            "-[:" + SENT_FRIEND_REQUEST + "]-> (:" + FRIEND_REQUEST + ") -[:" + FRIEND_REQUEST_TO + "]->" +
            "(:" + USER_REFERENCE + " {id: $receiverId})" +
            ")")
    boolean existsRequestBetween(@Param("requesterId") Long requesterId, @Param("receiverId") Long receiverId);

    List<FriendRequest> findAllByReceiver_IdAndStatus(Long receiverId, FriendRequestStatus status);
}
