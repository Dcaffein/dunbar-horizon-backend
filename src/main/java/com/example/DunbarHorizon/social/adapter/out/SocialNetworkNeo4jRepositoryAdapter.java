package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
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

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialNetworkNeo4jRepositoryAdapter implements SocialNetworkRepository {

    private final Neo4jClient neo4jClient;

    private static final Renderer renderer = Renderer.getRenderer(Configuration.defaultConfig());

    /**
     * 사용자의 기본 소셜 네트워크 지형도를 조회
     * 던바의 수 기반으로 노드 수를 제한(limitSize)하며
     * 관계의 친밀도(intimacy)와 사용자의 관심도(interestScore)를 한 번에 매핑하여 반환
     */
    @Override
    public List<NetworkFriendEdgeResult> getDefaultIntimacyNetwork(Long userId, int limitSize) {

        Node me = user().named("me");
        Node member = user().named("member");
        Node myFriendship = friendship().named("myFriendship");

        StatementBuilder.OngoingReading baseBuilder = matchUserWithId(userId, "me")
                .match(friendshipBetween(me, myFriendship, member));

        Statement statement = buildDynamicPruningNetwork(baseBuilder, me, member, myFriendship);

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bind(limitSize).to("limitSize")
                .bindAll(statement.getCatalog().getParameters())
                .fetchAs(NetworkFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new NetworkFriendEdgeResult(
                        record.get("friendA_Id").asLong(),
                        record.get("friendB_Id").asLong(),
                        record.get("intimacy").asDouble(0.0),
                        record.get("friendA_Interest").asDouble(0.0),
                        record.get("friendB_Interest").asDouble(0.0)
                ))
                .all()
                .stream()
                .toList();
    }

    /**
     * 특정 라벨(그룹)에 속한 멤버들로만 구성된 소셜 네트워크를 조회
     * 기본 네트워크와 동일한 동적 프루닝 및 데이터 매핑 정책을 공유
     */
    @Override
    public List<NetworkFriendEdgeResult> getLabelCustomNetwork(Long userId, String labelName, int limitSize) {

        Node me = user().named("me");
        Node label = label().named("label");
        Node member = user().named("member");
        Node myFriendship = friendship().named("myFriendship");

        StatementBuilder.OngoingReading baseBuilder = matchUserWithId(userId, "me")
                .match(me.relationshipTo(label, "HAS_LABEL").relationshipTo(member, "HAS_MEMBER"))
                .where(label.property("name").isEqualTo(Cypher.parameter("labelName")))
                .match(friendshipBetween(me, myFriendship, member));

        Statement statement = buildDynamicPruningNetwork(baseBuilder, me, member, myFriendship);

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bind(labelName).to("labelName")
                .bind(limitSize).to("limitSize")
                .bindAll(statement.getCatalog().getParameters())
                .fetchAs(NetworkFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new NetworkFriendEdgeResult(
                        record.get("friendA_Id").asLong(),
                        record.get("friendB_Id").asLong(),
                        record.get("intimacy").asDouble(0.0),
                        record.get("friendA_Interest").asDouble(0.0),
                        record.get("friendB_Interest").asDouble(0.0)
                ))
                .all()
                .stream()
                .toList();
    }

    /**
     * 네트워크 렌더링 시 발생하는 시각적 폭발을 방지하기 위해 엣지 개수를 동적으로 제한하는 공통 쿼리 빌더
     * 인가된 베이스 쿼리 위에서 컷오프 연산과 관심도 추출을 수행
     */
    private Statement buildDynamicPruningNetwork(
            StatementBuilder.OngoingReading baseBuilder,
            Node me, Node member, Node myFriendship) {

        Node innerFriendship = friendship().named("innerFriendship");
        Node targetMember = user().named("targetMember");
        Node friendshipA = friendship().named("friendshipA");
        Node friendshipB = friendship().named("friendshipB");
        Node targetNode = user().named("targetNode");

        Relationship relationshipA = me.relationshipTo(friendshipA, "HAS_FRIENDSHIP").named("relationshipA");
        Relationship relationshipB = me.relationshipTo(friendshipB, "HAS_FRIENDSHIP").named("relationshipB");

        SymbolicName boundary = Cypher.name("boundary");
        SymbolicName members = Cypher.name("members");
        SymbolicName friendshipList = Cypher.name("friendshipList");
        SymbolicName index = Cypher.name("index");
        SymbolicName dynamicLimit = Cypher.name("dynamicLimit");
        SymbolicName allEdges = Cypher.name("allEdges");
        SymbolicName topEdges = Cypher.name("topEdges");
        SymbolicName edgeData = Cypher.name("edgeData");

        // 탐색 경계 설정
        // 친밀도 기준 내림차순 정렬 후 요청된 크기(limitSize)만큼 멤버를 제한하여 컬렉션으로 집계
        StatementBuilder.OngoingReadingAndWith withBoundary = baseBuilder
                .with(me, member, myFriendship)
                .orderBy(myFriendship.property(PROP_INTIMACY).descending())
                .limit(Cypher.parameter("limitSize"))
                .with(
                        me,
                        Cypher.collect(member.getRequiredSymbolicName()).as("members"),
                        Cypher.collect(myFriendship.getRequiredSymbolicName()).as("friendshipList")
                )
                .with(
                        me,
                        Cypher.call("apoc.coll.union")
                                .withArgs(members, Cypher.listOf(me.getRequiredSymbolicName()))
                                .asFunction().as("boundary"),
                        members,
                        friendshipList
                );

        // 동적 한계치 연산
        // 노드별로 시각화될 엣지의 수를 사용자와의 친밀도에 비례하여 동적으로 할당 (5~30개)
        Expression myIntimacy = Cypher.coalesce(myFriendship.property(PROP_INTIMACY), Cypher.literalOf(0.0));
        Expression limitCalc = Cypher.call("toInteger").withArgs(
                Cypher.literalOf(5).add(myIntimacy.multiply(Cypher.literalOf(25)))
        ).asFunction();

        StatementBuilder.OngoingReadingAndWith withLimit = withBoundary
                .unwind(
                        Cypher.call("range")
                                .withArgs(Cypher.literalOf(0), Cypher.size(members).subtract(Cypher.literalOf(1)))
                                .asFunction()
                ).as("index")
                .with(
                        me,
                        boundary,
                        Cypher.valueAt(members, index).as(member.getRequiredSymbolicName().getValue()),
                        Cypher.valueAt(friendshipList, index).as(myFriendship.getRequiredSymbolicName().getValue())
                )
                .with(me, boundary, member, limitCalc.as("dynamicLimit"));

        Expression edgeMap = Cypher.mapOf(
                "innerFriendship", innerFriendship.getRequiredSymbolicName(),
                "targetMember", targetMember.getRequiredSymbolicName()
        );

        // 관계 매칭 및 프루닝 적용
        // 확정된 바운더리 내부에서 노드 간의 관계를 매칭하고, 각 노드별로 계산된 동적 한계치까지만 엣지를 슬라이스
        return withLimit
                .match(friendshipBetween(member, innerFriendship, targetMember))
                .where(targetMember.getRequiredSymbolicName().in(boundary))
                .with(me, member, dynamicLimit, innerFriendship, targetMember)
                .orderBy(innerFriendship.property(PROP_INTIMACY).descending())
                .with(
                        me,
                        member,
                        dynamicLimit,
                        Cypher.collect(edgeMap).as("allEdges")
                )
                .with(
                        me,
                        member,
                        Cypher.subList(allEdges, Cypher.literalOf(0), dynamicLimit).as("topEdges")
                )

                // 슬라이싱된 엣지 데이터를 다시 행으로 전개하고, 맵의 요소를 정식 노드로 승격시켜 매칭에 사용
                .unwind(topEdges).as("edgeData")
                .with(
                        me, member, edgeData,
                        Cypher.property(edgeData, "targetMember").as(targetNode.getRequiredSymbolicName().getValue())
                )

                // 중심 노드(me)와 연결된 각 친구 노드의 interestScore를 조회하여 최종 결과에 포함
                .match(relationshipA.relationshipFrom(member, "HAS_FRIENDSHIP"))
                .match(relationshipB.relationshipFrom(targetNode, "HAS_FRIENDSHIP"))
                .returning(
                        member.property(PROP_ID).as("friendA_Id"),
                        targetNode.property(PROP_ID).as("friendB_Id"),
                        Cypher.property(Cypher.property(edgeData, "innerFriendship"), PROP_INTIMACY).as("intimacy"),
                        relationshipA.property("interestScore").as("friendA_Interest"),
                        relationshipB.property("interestScore").as("friendB_Interest")
                )
                .build();
    }

    /**
     * 알 수도 있는 사람(2-Hop 타겟)과 나와의 공통 친구를 조회
     * 논리적 위계질서를 유지하기 위해, 현재 시각화된 화면 인원 내에서만 검색하며
     * 도출되는 엣지 수는 타겟의 비친밀 상태를 고려하여 최대 5개로 제한
     */
    @Override
    public List<NetworkOneHopsByTwoHopResult> getIntersectionOneHops(
            Long userId, Long targetId, String labelName, int limitSize) {

        Node targetUser = user().named("targetUser");
        Node mutual = user().named("mutual");
        Node targetFriendship = friendship().named("targetFriendship");
        Relationship targetRelationship = targetUser.relationshipTo(targetFriendship, HAS_FRIENDSHIP).named("targetRelationship");
        SymbolicName currentSkeleton = Cypher.name("currentSkeleton");

        // 1. 공통 빌더를 호출하여 현재 화면 상태(currentSkeleton) 재구성
        StatementBuilder.OngoingReadingAndWith withSkeleton = buildCurrentSkeleton(userId, labelName);

        // 2. 이방인 페널티 적용하여 최대 5명 도출
        Statement statement = withSkeleton
                .match(targetUser).where(idEquals(targetUser, targetId))
                .match(targetRelationship.relationshipFrom(mutual, HAS_FRIENDSHIP))
                .where(isRoutable(targetRelationship).and(mutual.getRequiredSymbolicName().in(currentSkeleton)))
                .with(mutual, targetFriendship)
                .orderBy(targetFriendship.property(PROP_INTIMACY).descending())
                .limit(5)
                .returning(mutual.property(PROP_ID).as("friendId"))
                .build();

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bind(targetId).to("targetId")
                .bind(limitSize).to("limitSize")
                .bind(labelName == null ? "" : labelName).to("labelName")
                .fetchAs(NetworkOneHopsByTwoHopResult.class)
                // ... mappedBy 로직 동일
                .all().stream().toList();
    }

    /**
     * 내 친구와 네트워크의 연결을 조회
     */
    @Override
    public List<MutualFriendEdgeResult> getIntersectionByOneHop(
            Long userId, Long targetId, String labelName, int limitSize) {

        Node target = user().named("target");
        Node mutual = user().named("mutual");
        Node targetFriendship = friendship().named("targetFriendship");
        Relationship targetRelationship = target.relationshipTo(targetFriendship, HAS_FRIENDSHIP).named("targetRelationship");
        SymbolicName currentSkeleton = Cypher.name("currentSkeleton");

        // 1. 공통 빌더를 호출하여 현재 화면 상태(currentSkeleton) 재구성
        StatementBuilder.OngoingReadingAndWith withSkeleton = buildCurrentSkeleton(userId, labelName);

        // 2. 타겟과 교집합 찾기
        Statement statement = withSkeleton
                .match(target).where(idEquals(target, targetId))
                .match(targetRelationship.relationshipFrom(mutual, HAS_FRIENDSHIP))
                // 💡 핵심: 백엔드가 직접 만든 currentSkeleton 안에 있는 사람만 교집합으로 인정!
                .where(mutual.getRequiredSymbolicName().in(currentSkeleton))
                .and(isRoutable(targetRelationship))
                .returning(
                        target.property(PROP_ID).as("friendAId"),
                        mutual.property(PROP_ID).as("friendBId"),
                        targetFriendship.property(PROP_INTIMACY).as("intimacy")
                )
                .build();

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bind(targetId).to("targetId")
                .bind(limitSize).to("limitSize")
                // labelName 파라미터 바인딩 (null이어도 무방)
                .bind(labelName == null ? "" : labelName).to("labelName")
                .fetchAs(MutualFriendEdgeResult.class)
                // ... mappedBy 로직 동일
                .all().stream().toList();
    }

    /**
     * 클라이언트가 현재 보고 있는 화면의 뼈대(Skeleton)를 백엔드에서 결정론적으로 재구성합니다.
     * labelName이 존재하면 해당 라벨로 필터링하고, 없으면 글로벌 네트워크 기준으로 limitSize만큼 자릅니다.
     */
    private StatementBuilder.OngoingReadingAndWith buildCurrentSkeleton(Long userId, String labelName) {
        Node me = user().named("me");
        Node myFriend = user().named("myFriend");
        Node myFriendship = friendship().named("myFriendship");
        StatementBuilder.OngoingReading builder = matchUserWithId(userId, "me")
                .match(friendshipBetween(me, myFriendship, myFriend));

        // 라벨 컨텍스트가 주어졌다면, 라벨 필터링 추가
        if (labelName != null && !labelName.isBlank()) {
            Node label = label().named("label");
            builder = builder.match(me.relationshipTo(label, "HAS_LABEL").relationshipTo(myFriend, "HAS_MEMBER"))
                    .where(label.property("name").isEqualTo(Cypher.parameter("labelName")));
        }

        // 친밀도 순 정렬 및 limitSize 적용 후, 하나의 리스트(currentSkeleton)로 집계
        return builder.with(myFriend, myFriendship)
                .orderBy(myFriendship.property(PROP_INTIMACY).descending())
                .limit(Cypher.parameter("limitSize"))
                .with(Cypher.collect(myFriend).as("currentSkeleton"));
    }
}