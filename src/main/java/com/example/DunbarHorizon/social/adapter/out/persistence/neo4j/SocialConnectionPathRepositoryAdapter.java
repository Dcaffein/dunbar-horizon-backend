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

    // score는 정렬 기준으로만 쓰고 반환하지 않는다. 남 두 사람의 친밀도를 제3자에게 주지 않기 위함
    // collect 앞의 ORDER BY가 리스트 순서를 만들고, size()는 자르기 전에 세어 전체 수를 낸다
    private static final String INTERMEDIARIES_QUERY = """
            MATCH (me:UserReference {id: $myId})-[f1:FRIENDS_WITH]-(mid:UserReference)
                  -[f2:FRIENDS_WITH]-(target:UserReference {id: $targetId})
            WHERE CASE WHEN startNode(f1) = mid THEN f1.lowIsRoutable ELSE f1.highIsRoutable END = true
              AND CASE WHEN startNode(f2) = target THEN f2.lowIsRoutable ELSE f2.highIsRoutable END = true
            WITH mid, sqrt(f1.intimacy * f2.intimacy) AS score
            ORDER BY score DESC
            WITH collect({userId: mid.id, nickname: mid.nickname}) AS ranked
            RETURN ranked[0..$limit] AS intermediaries, size(ranked) AS totalCount
            """;

    @Override
    public ConnectionPathResult.Intermediaries findIntermediaries(Long myId, Long targetId, int limit) {
        return neo4jClient.query(INTERMEDIARIES_QUERY)
                .bind(myId).to("myId")
                .bind(targetId).to("targetId")
                .bind(limit).to("limit")
                .fetchAs(ConnectionPathResult.Intermediaries.class)
                .mappedBy((typeSystem, record) -> new ConnectionPathResult.Intermediaries(
                        record.get("intermediaries").asList(value ->
                                new ConnectionPathResult.IntermediaryResult(
                                        value.get("userId").asLong(),
                                        value.get("nickname").asString()
                                )),
                        record.get("totalCount").asInt()
                ))
                .one()
                .orElseGet(() -> new ConnectionPathResult.Intermediaries(List.of(), 0));
    }
}
