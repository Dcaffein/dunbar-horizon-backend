package com.example.GooRoomBe.social.adapter.out.neo4j.springData;

import com.example.GooRoomBe.social.domain.friend.FriendRequest;
import com.example.GooRoomBe.social.domain.friend.FriendRequestStatus;
import lombok.NonNull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.example.GooRoomBe.social.domain.friend.constant.FriendConstants.*;
import static com.example.GooRoomBe.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public interface FriendRequestNeo4jRepository extends Neo4jRepository<FriendRequest, String> {

    @NonNull
    @Override
    Optional<FriendRequest> findById(@NonNull String id);

    @Query("MATCH (req: " + USER_REFERENCE + " {id: $requesterId}), (rec:" + USER_REFERENCE + " {id: $receiverId}) " +
            "MERGE (req)-[:" + SENT + "]->(fr:" + FRIEND_REQUEST + ")-[:" + TO + "]->(rec) " +
            "ON CREATE SET " +
            "  fr.id = $requestId, " +
            "  fr.status = 'PENDING', " +
            "  fr.createdAt = datetime() " +
            "RETURN fr")
    FriendRequest mergeFriendRequest(
            @Param("requesterId") Long requesterId,
            @Param("receiverId") Long receiverId,
            @Param("requestId") String requestId
    );

    @Query("MATCH (n:" + FRIEND_REQUEST + " {id: :#{#entity.id}}) SET n = :#{#entity} RETURN n")
    FriendRequest updateFriendRequest(@Param("entity") FriendRequest entity);

    @Query("RETURN EXISTS(" +
            "(:" + USER_REFERENCE + " {id: $requesterId})" +
            "-[:" + SENT + "]-> (:" + FRIEND_REQUEST + ") -[:" + TO + "]->" +
            "(:" + USER_REFERENCE + " {id: $receiverId})" +
            ")")
    boolean existsRequestBetween(@Param("requesterId") Long requesterId, @Param("receiverId") Long receiverId);

    List<FriendRequest> findAllByReceiver_IdAndStatus(Long receiverId, FriendRequestStatus status);
}
