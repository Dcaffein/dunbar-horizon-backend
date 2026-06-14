package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j;

import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult;
import com.example.DunbarHorizon.social.application.port.out.SocialConnectionPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.schema.SocialGraphSchema.*;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.FRIENDSHIP;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.HAS_FRIENDSHIP;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialConnectionPathRepositoryAdapter implements SocialConnectionPathRepository {

    private final Neo4jClient neo4jClient;

    private static final String INTERMEDIARIES_QUERY = ("""
            MATCH (me:#{UR} {#{ID}: $myId})-[:#{HF}]->(f1:#{F})<-[r2:#{HF}]-(mid:#{UR})
                  -[r3:#{HF}]->(f2:#{F})<-[r4:#{HF}]-(target:#{UR} {#{ID}: $targetId})
            WHERE r2.#{IR} = true AND r4.#{IR} = true
            WITH mid, sqrt(f1.#{INTIMACY} * f2.#{INTIMACY}) AS score
            ORDER BY score DESC
            RETURN mid.#{ID} AS userId, mid.#{NICK} AS nickname, score
            """)
            .replace("#{UR}", USER_REFERENCE)
            .replace("#{F}", FRIENDSHIP)
            .replace("#{HF}", HAS_FRIENDSHIP)
            .replace("#{ID}", PROP_ID)
            .replace("#{NICK}", PROP_NICKNAME)
            .replace("#{INTIMACY}", PROP_INTIMACY)
            .replace("#{IR}", PROP_IS_ROUTABLE);

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
