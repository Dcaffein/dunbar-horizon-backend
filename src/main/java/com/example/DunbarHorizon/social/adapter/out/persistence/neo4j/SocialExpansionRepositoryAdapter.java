package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.port.out.SocialExpansionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SocialExpansionRepositoryAdapter implements SocialExpansionRepository {

    private final Neo4jClient neo4jClient;

    private static final String ANCHOR_EXPANSION_QUERY = """
            // [1] me와 anchor 확인, me의 1-hop 친구 목록 수집 (anchor 제외)
            MATCH (me:UserReference {id: $meId})
            MATCH (anchor:UserReference {id: $anchorId})
            MATCH (me)-[:FRIENDS_WITH]-(myFriendNode:UserReference)
            WHERE myFriendNode.id <> anchor.id
            WITH me, anchor, collect(distinct myFriendNode) AS myFriends

            // [2] anchor가 소유하고 me가 속한 라벨 수집 (맥락 유사도 기준)
            OPTIONAL MATCH (anchor)-[:OWNS_LABEL]->(sharedLabel:Label)-[:ATTACHED_TO]->(me)
            WITH me, anchor, myFriends, collect(distinct sharedLabel) AS mySharedLabels

            // [3] anchor의 2-hop 후보 탐색, isRoutable 필터 + 친구 범위 조건 적용 후 친밀도 내림차순 정렬
            MATCH (anchor)-[targetFriendship:FRIENDS_WITH]-(target:UserReference)
            WHERE target.id <> me.id
              AND CASE WHEN startNode(targetFriendship) = target
                       THEN targetFriendship.lowIsRoutable
                       ELSE targetFriendship.highIsRoutable END = true
              AND ($onlyMyFriends = (target IN myFriends))
            WITH target, targetFriendship, me, anchor, myFriends, mySharedLabels
            ORDER BY targetFriendship.intimacy DESC

            // [4] 공통 지인 수 산출 (신뢰도 측정)
            CALL (target, myFriends, me, anchor) {
              MATCH (target)-[:FRIENDS_WITH]-(mutual:UserReference)
              WHERE mutual IN myFriends
                AND mutual.id <> me.id
                AND mutual.id <> anchor.id
              RETURN count(distinct mutual) AS mutualCount
            }

            // [5] 공유 라벨 수 산출 (맥락 유사도 가중치)
            WITH target, targetFriendship, mutualCount, mySharedLabels, anchor
            WITH target, targetFriendship, mutualCount,
                 size([(anchor)-[:OWNS_LABEL]->(tl:Label)-[:ATTACHED_TO]->(target) WHERE tl IN mySharedLabels | tl]) AS labelCount

            // [6] threshold 이상인 후보만 반환
            WHERE mutualCount + labelCount >= $threshold
            RETURN target.id       AS id,
                   target.nickname     AS nickname
            LIMIT $limitCount
            """;

    @Override
    public List<AnchorExpansionResult> getRelatedNetworkByAnchor(Long meId, Long anchorId, int threshold, int limitCount) {
        return executeQuery(meId, anchorId, threshold, limitCount, true);
    }

    @Override
    public List<AnchorExpansionResult> getRecommendedNetworkByAnchor(Long meId, Long anchorId, int threshold, int limitCount) {
        return executeQuery(meId, anchorId, threshold, limitCount, false);
    }

    private List<AnchorExpansionResult> executeQuery(Long meId, Long anchorId, int threshold, int limitCount, boolean onlyMyFriends) {
        return neo4jClient.query(ANCHOR_EXPANSION_QUERY)
                .bind(meId).to("meId")
                .bind(anchorId).to("anchorId")
                .bind(threshold).to("threshold")
                .bind(limitCount).to("limitCount")
                .bind(onlyMyFriends).to("onlyMyFriends")
                .fetchAs(AnchorExpansionResult.class)
                .mappedBy((typeSystem, record) -> new AnchorExpansionResult(
                        record.get("id").asLong(),
                        record.get("nickname").asString()
                ))
                .all()
                .stream()
                .toList();
    }
}
