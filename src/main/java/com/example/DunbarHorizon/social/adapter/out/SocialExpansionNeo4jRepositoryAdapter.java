package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.port.out.SocialExpansionRepository;
import lombok.RequiredArgsConstructor;
import org.neo4j.cypherdsl.core.*;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.SocialNetworkPatterns.*;
import static com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.SocialNetworkProperties.*;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.HAS_FRIENDSHIP;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.ATTACHED_TO;
import static org.neo4j.cypherdsl.core.Cypher.*;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialExpansionNeo4jRepositoryAdapter implements SocialExpansionRepository {

    private final Neo4jClient neo4jClient;

    private static final Renderer renderer = Renderer.getRenderer(Configuration.defaultConfig());

    @Override
    public List<AnchorExpansionResult> getRelatedNetworkByAnchor(Long meId, Long anchorId, int threshold, int limitCount) {
        // excludeMyFriends = false: 기존 친구를 포함하여 앵커 주변의 연관 네트워크 전체를 탐색
        return executeAnchorExpansionQuery(meId, anchorId, threshold, limitCount, false);
    }

    @Override
    public List<AnchorExpansionResult> getRecommendedNetworkByAnchor(Long meId, Long anchorId, int threshold, int limitCount) {
        // excludeMyFriends = true: 이미 친구인 유저를 배제하여 '새로운 인맥 추천' 목적으로만 탐색
        return executeAnchorExpansionQuery(meId, anchorId, threshold, limitCount, true);
    }

    /**
     * 특정 앵커(친구)를 기준으로 2-Hop 네트워크를 탐색하고, 공통 지인 및 공유 라벨(맥락)을 기반으로 연관성 점수를 산출하여 반환합니다.
     */
    private List<AnchorExpansionResult> executeAnchorExpansionQuery(Long meId, Long anchorId, int threshold, int limitCount, boolean excludeMyFriends) {

        Node me = user().named("me");
        Node anchor = user().named("anchor");
        Node myFriendNode = user().named("myFriendNode");
        Node sharedLabel = label().named("sharedLabel");
        Node target = user().named("target");
        Node targetFriendship = friendship().named("targetFriendship");
        Node targetLabel = label().named("targetLabel");

        SymbolicName myFriends = name("myFriends");
        SymbolicName mySharedLabels = name("mySharedLabels");
        SymbolicName mutualCount = name("mutualCount");
        SymbolicName labelCount = name("labelCount");

        Relationship targetToAnchorRel = targetFriendship.relationshipFrom(target, HAS_FRIENDSHIP).named("targetRel");

        // 기준 네트워크 경계(Boundary) 설정
        // 후속 교집합 연산(공통 친구 찾기)의 기준이 될 현재 사용자의 1-Hop 인맥 풀을 확보
        // 사용자(me)와 앵커(anchor)의 유효성을 검증하고, 나와 앵커를 제외한 모든 친구를 myFriends 리스트로 집계
        StatementBuilder.OngoingReadingAndWith withMyFriends = match(me)
                .where(me.property(PROP_ID).isEqualTo(parameter("meId").withValue(meId)))
                .match(anchor)
                .where(anchor.property(PROP_ID).isEqualTo(parameter("anchorId").withValue(anchorId)))
                .match(friendshipBetween(me, friendship(), myFriendNode))
                .where(isNotSameNode(myFriendNode, anchor))
                .with(me, anchor, collectDistinct(myFriendNode).as(myFriends));

        // 교집합 추출
        // 앵커가 나에게 부여한 사회적 맥락을 타겟과의 유사도 평가에 반영할 준비
        // 앵커가 소유하고 동시에 나를 멤버로 지정한 라벨 노드들을 mySharedLabels 리스트로 집계
        StatementBuilder.OngoingReadingAndWith withLabels = withMyFriends
                .optionalMatch(ownsLabel(anchor, sharedLabel).relationshipTo(me, ATTACHED_TO))
                .with(me, anchor, myFriends, collectDistinct(sharedLabel).as(mySharedLabels));

        // 탐색 후보군 필터링 정책 정의
        // 타겟 유저의 프라이버시 권한(노출 허용 여부)과 쿼리 목적(추천 vs 단순 조회)에 따라 탐색 대상을 제한
        // 기본적으로 본인(me)을 제외하고 isRoutable 속성이 true인 관계만 허용하며
        //  excludeMyFriends 플래그에 따라 myFriends 리스트 포함 여부를 조건에 추가
        Condition targetCondition = isNotSameNode(target, me)
                .and(isRoutable(targetToAnchorRel));

        if (excludeMyFriends) {
            targetCondition = targetCondition.and(target.getRequiredSymbolicName().in(myFriends).not());
        }

        // 2-Hop 타겟 탐색 및 우선순위 정렬
        // 앵커의 인맥 풀을 조회하여 필터링을 거친 후, 앵커와 가장 친밀한 사람부터 추천 후보로 올리기 위해 정렬
        // 앵커와 타겟 간의 HAS_FRIENDSHIP 관계를 매칭하고
        //  앞서 정의한 targetCondition을 적용한 뒤 앵커-타겟 간의 intimacy를 기준으로 내림차순 정렬
        StatementBuilder.OngoingReadingAndWith withSortedTargets = withLabels
                .match(anchor.relationshipTo(targetFriendship, HAS_FRIENDSHIP), targetToAnchorRel)
                .where(targetCondition)
                .with(target, targetFriendship, me, anchor, myFriends, mySharedLabels)
                .orderBy(targetFriendship.property(PROP_INTIMACY).descending());

        // 공통 지인 수(Mutual Friends) 연산
        // 타겟과 내가 현실 세계에서 연결되어 있을 확률(신뢰도)을 측정하기 위해 겹치는 지인의 수를 계산
        // 타겟의 1-Hop 친구(mutual) 중 내 친구 목록(myFriends)에 포함되는 노드만 추출하여
        //  중복 없이(DISTINCT) 개수를 산출하는 인라인 서브쿼리를 실행
        Node fs = friendship().named("fs");
        Node mutual = user().named("mutual");

        Statement pureCypherSubquery = Cypher.with(target, myFriends, me, anchor)
                .match(target.relationshipTo(fs, HAS_FRIENDSHIP).relationshipFrom(mutual, HAS_FRIENDSHIP))
                .where(mutual.getRequiredSymbolicName().in(myFriends))
                .and(mutual.isNotEqualTo(me))
                .and(mutual.isNotEqualTo(anchor))
                .returning(Cypher.countDistinct(mutual).as(mutualCount))
                .build();

        // 공유 라벨 가중치 연산
        // 공통 지인이 부족하더라도, 나와 타겟이 앵커가 같은 클러스터로 인지한다면(같은 라벨에 속한다면) 추천 가중치를 부여
        // 타겟에게 부여된 라벨 중 2단계에서 추출한 mySharedLabels에 포함되는 라벨을 필터링하여 리스트로 평가
        Expression labelComprehension = listBasedOn(ownsLabel(anchor, targetLabel).relationshipTo(target, ATTACHED_TO))
                .where(targetLabel.getRequiredSymbolicName().in(mySharedLabels))
                .returning(targetLabel);

        // 최종 연관성 평가 및 결과 프로젝션
        // 공통 지인 수와 공유 맥락 수의 합산 점수가 시스템 최소 요구치(Threshold)를 넘는 타겟만 최종 추천/조회 결과로 반환
        // 서브쿼리 결과(mutualCount)와 라벨 리스트의 크기(labelCount)를 더해 threshold 파라미터와 비교하고
        //  조건을 만족하는 노드의 속성들을 DTO 매핑을 위해 반환 및 limit 처리합니다.
        Statement statement = withSortedTargets
                .call(pureCypherSubquery)
                .with(target, targetFriendship, mutualCount, mySharedLabels, anchor)
                .with(target, targetFriendship, mutualCount, size(labelComprehension).as(labelCount))
                .where(mutualCount.add(labelCount).gte(parameter("threshold").withValue(threshold)))
                .returning(
                        target.property(PROP_ID).as("id"),
                        target.property(PROP_NICKNAME).as("nickname"),
                        targetFriendship.property(PROP_INTIMACY).as("intimacy"),
                        mutualCount.as("mutualCount"),
                        labelCount.as("labelCount")
                )
                .limit(parameter("limitCount").withValue(limitCount))
                .build();

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bindAll(statement.getCatalog().getParameters())
                .fetchAs(AnchorExpansionResult.class)
                .mappedBy((typeSystem, record) -> new AnchorExpansionResult(
                        record.get("id").asLong(),
                        record.get("nickname").asString(),
                        record.get("intimacy").asDouble(0.0),
                        record.get("mutualCount").asInt(0),
                        record.get("labelCount").asInt(0)
                ))
                .all()
                .stream()
                .toList();
    }
}