package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.ApocPatterns;
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
        // excludeMyFriends = false
        return executeAnchorExpansionQuery(meId, anchorId, threshold, limitCount, false);
    }

    @Override
    public List<AnchorExpansionResult> getRecommendedNetworkByAnchor(Long meId, Long anchorId, int threshold, int limitCount) {
        // excludeMyFriends = true
        return executeAnchorExpansionQuery(meId, anchorId, threshold, limitCount, true);
    }

    /**
     내 친구 anchor의 친구(targets) 중에서 나와 관련도(라벨,나 와의 공통 친구)가 있는 친구를 탐색
     **/
    private List<AnchorExpansionResult> executeAnchorExpansionQuery(Long meId, Long anchorId, int threshold, int limitCount, boolean excludeMyFriends) {

        // 쿼리 조립에 사용될 그래프 노드 및 심볼릭 네임 정의
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

        // 타겟 사용자가 기준점(anchor)을 통해 자신을 노출할지 결정하는 프라이버시 설정 확인용 관계 정의
        Relationship targetToAnchorRel = targetFriendship.relationshipFrom(target, HAS_FRIENDSHIP).named("targetRel");

        // 1. 현재 사용자의 친구 목록을 수집
        StatementBuilder.OngoingReadingAndWith withMyFriends = match(me)
                .where(me.property(PROP_ID).isEqualTo(parameter("meId").withValue(meId)))
                .match(anchor)
                .where(anchor.property(PROP_ID).isEqualTo(parameter("anchorId").withValue(anchorId)))
                .match(friendshipBetween(me, friendship(), myFriendNode))
                .where(isNotSameNode(myFriendNode, anchor))
                .with(me, anchor, collectDistinct(myFriendNode).as(myFriends));

        // 2. 기준점(anchor)이 소유하고 현재 사용자가 포함된 라벨 목록 수집
        StatementBuilder.OngoingReadingAndWith withLabels = withMyFriends
                .optionalMatch(ownsLabel(anchor, sharedLabel).relationshipTo(me, ATTACHED_TO))
                .with(me, anchor, myFriends, collectDistinct(sharedLabel).as(mySharedLabels));

        // 3. 필터링 조건 설정
        // 기본 필터링: 본인 제외 및 타겟이 기준점에게 부여한 라우팅 권한(isRoutable) 확인
        Condition targetCondition = isNotSameNode(target, me)
                .and(isRoutable(targetToAnchorRel));
        // 추천 기능인 경우 이미 친구인 유저는 결과에서 제외하도록 필터 추가
        if (excludeMyFriends) {
            targetCondition = targetCondition.and(target.getRequiredSymbolicName().in(myFriends).not());
        }

        // 4. 기준점의 친구(타겟)를 탐색하고 위에서 설정한 필터 적용 후 친밀도 높은 순으로 정렬
        StatementBuilder.OngoingReadingAndWith withSortedTargets = withLabels
                .match(anchor.relationshipTo(targetFriendship, HAS_FRIENDSHIP), targetToAnchorRel)
                .where(targetCondition)
                .with(target, targetFriendship, me, anchor, myFriends, mySharedLabels)
                .orderBy(targetFriendship.property(PROP_INTIMACY).descending());

        // 5. 캐싱된 친구 목록을 메모리 해시 조인 방식으로 대조하여 타겟과의 공통 친구 수 계산
        Statement apocSubquery = ApocPatterns.countMutualFriendsSubquery(
                target.getRequiredSymbolicName(),
                myFriends,
                me.getRequiredSymbolicName(),
                anchor.getRequiredSymbolicName(),
                mutualCount
        );

        // 6. 타겟과 기준점 사이의 라벨 중 사용자가 공유 중인 라벨 개수 산출
        Expression labelComprehension = listBasedOn(ownsLabel(anchor, targetLabel).relationshipTo(target, ATTACHED_TO))
                .where(targetLabel.getRequiredSymbolicName().in(mySharedLabels))
                .returning(targetLabel);

        // 7. 공통 친구와 공유 라벨 수의 합산이 임계치를 넘는 데이터만 필터링하여 최종 반환
        Statement statement = withSortedTargets
                .call(apocSubquery)
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

        // 자동 파라미터 바인딩을 통해 렌더링된 쿼리 실행 및 결과 객체 매핑
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
