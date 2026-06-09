package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j;

import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult;
import com.example.DunbarHorizon.social.application.port.out.SocialConnectionPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialConnectionPathRepositoryAdapter implements SocialConnectionPathRepository {

    private final Neo4jClient neo4jClient;

    private static final String INTERMEDIARIES_QUERY = """
            MATCH (me:UserReference {id: $myId})-[:HAS_FRIENDSHIP]->(f1:Friendship)<-[r2:HAS_FRIENDSHIP]-(mid:UserReference)
                  -[r3:HAS_FRIENDSHIP]->(f2:Friendship)<-[r4:HAS_FRIENDSHIP]-(target:UserReference {id: $targetId})
            WHERE r2.isRoutable = true AND r4.isRoutable = true
            WITH mid, sqrt(f1.intimacy * f2.intimacy) AS score
            ORDER BY score DESC
            RETURN mid.id AS userId, mid.nickname AS nickname, score
            """;

    @Override
    public List<ConnectionPathResult.IntermediaryResult> findIntermediaries(Long myId, Long targetId) {
        return neo4jClient.query(INTERMEDIARIES_QUERY)
                .bind(myId).to("myId")
                .bind(targetId).to("targetId")
                .fetchAs(ConnectionPathResult.IntermediaryResult.class)
                .mappedBy((typeSystem, record) -> new ConnectionPathResult.IntermediaryResult(
                        record.get("userId").asLong(),
                        record.get("nickname").asString(),
                        record.get("score").asDouble(0.0)
                ))
                .all()
                .stream()
                .toList();
    }
}
