package com.example.GooRoomBe.social.trace.infrastructure;

import com.example.GooRoomBe.social.trace.domain.Trace;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.GooRoomBe.social.common.SocialSchemaConstants.*;

public interface TraceRepository extends Neo4jRepository<Trace, String> {

    @Query("MATCH (v:" + SOCIAL_USER + " {id: $visitorId})-[r1:LEFT]->(trace:Trace)-[r2:ON]->(t:" + SOCIAL_USER + " {id: $targetId}) " +
            "RETURN trace, r1, v, r2, t")
    Optional<Trace> findByVisitorIdAndTargetId(@Param("visitorId") String visitorId, @Param("targetId") String targetId);

    @Query("MATCH (v:" + SOCIAL_USER + " {id: $visitorId}) " +
            "MATCH (t:" + SOCIAL_USER + " {id: $targetId}) " +
            "OPTIONAL MATCH (v)-[:" + LEFT + "]->(trace:" + TRACE + ")-[:" + ON + "]->(t) " +
            "RETURN COALESCE(trace.count, 0)")
    int getVisitCount(@Param("visitorId") String visitorId, @Param("targetId") String targetId);


    @Query("MATCH (v:" + SOCIAL_USER + " {id: $visitorId}) " +
            "MATCH (t:" + SOCIAL_USER + " {id: $targetId}) " +
            "MERGE (v)-[:" + LEFT + "]->(trace:" + TRACE + ")-[:" + ON + "]->(t) " +
            "ON CREATE SET trace.id = randomUUID(), trace.count = $count, trace.lastVisitedAt = $lastVisitedAt " +
            "ON MATCH SET trace.count = $count, trace.lastVisitedAt = $lastVisitedAt")
    void saveTrace(@Param("visitorId") String visitorId,
                   @Param("targetId") String targetId,
                   @Param("count") int count,
                   @Param("lastVisitedAt") LocalDateTime lastVisitedAt);
}