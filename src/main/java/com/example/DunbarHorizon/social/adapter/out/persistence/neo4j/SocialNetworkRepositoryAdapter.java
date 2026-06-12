package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import lombok.RequiredArgsConstructor;
import org.neo4j.cypherdsl.core.*;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.dsl.SocialNetworkPatterns.*;
import static com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.dsl.SocialNetworkProperties.*;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialNetworkRepositoryAdapter implements SocialNetworkRepository {

    private final Neo4jClient neo4jClient;

    private static final Renderer renderer = Renderer.getRenderer(Configuration.defaultConfig());

    @Cacheable(cacheNames = "dunbar:network:default", key = "#userId + ':' + #circleSize.name()")
    @Override
    public List<NetworkFriendEdgeResult> getDefaultIntimacyNetwork(Long userId, DunbarCircle circleSize) {

        Node me = user().named("me");
        Node member = user().named("member");
        Node myFriendship = friendship().named("myFriendship");
        Relationship rMe = me.relationshipTo(myFriendship, "HAS_FRIENDSHIP").named("r_me");

        StatementBuilder.OngoingReading baseBuilder = matchUserWithId(userId, "me")
                .match(rMe.relationshipFrom(member, "HAS_FRIENDSHIP"));

        Statement statement = buildDynamicPruningNetwork(baseBuilder, me, member, myFriendship, rMe);

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bind(circleSize.getLimitSize()).to("limitSize")
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

    @Cacheable(cacheNames = "dunbar:network:label", key = "#userId + ':' + #labelId")
    @Override
    public List<NetworkFriendEdgeResult> getLabelCustomNetwork(Long userId, String labelId) {

        Node me = user().named("me");
        Node label = label().named("label");
        Node member = user().named("member");
        Node myFriendship = friendship().named("myFriendship");
        Relationship rMe = me.relationshipTo(myFriendship, "HAS_FRIENDSHIP").named("r_me");

        StatementBuilder.OngoingReading baseBuilder = matchUserWithId(userId, "me")
                .match(me.relationshipTo(label, "HAS_LABEL").relationshipTo(member, "HAS_MEMBER"))
                .where(label.property("id").isEqualTo(Cypher.parameter("labelId")))
                .match(rMe.relationshipFrom(member, "HAS_FRIENDSHIP"));

        Statement statement = buildDynamicPruningNetwork(baseBuilder, me, member, myFriendship, rMe);

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bind(labelId).to("labelId")
                .bind(DunbarCircle.DUNBAR.getLimitSize()).to("limitSize")
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

    private Statement buildDynamicPruningNetwork(
            StatementBuilder.OngoingReading baseBuilder,
            Node me, Node member, Node myFriendship, Relationship rMe) {

        Node innerFriendship = friendship().named("innerFriendship");
        Node targetMember = user().named("targetMember");

        SymbolicName boundary = Cypher.name("boundary");
        SymbolicName friendData = Cypher.name("friendData");
        SymbolicName interestMap = Cypher.name("interestMap");
        SymbolicName dynamicLimit = Cypher.name("dynamicLimit");
        SymbolicName topEdges = Cypher.name("topEdges");
        SymbolicName edgeData = Cypher.name("edgeData");
        SymbolicName item = Cypher.name("item");
        SymbolicName x = Cypher.name("x");

        // Step A: ORDER BY intimacy LIMIT $limitSize 후 friendData map으로 수집
        Expression friendDataElement = Cypher.mapOf(
                "member", member.getRequiredSymbolicName(),
                "friendship", myFriendship.getRequiredSymbolicName(),
                "interestScore", Cypher.coalesce(rMe.property(PROP_INTEREST_SCORE), Cypher.literalOf(0.0))
        );

        StatementBuilder.OngoingReadingAndWith withFriendData = baseBuilder
                .with(me, member, myFriendship, rMe)
                .orderBy(myFriendship.property(PROP_INTIMACY).descending())
                .limit(Cypher.parameter("limitSize"))
                .with(me, Cypher.collect(friendDataElement).as("friendData"));

        // Step B: boundary = union(members, [me]), interestMap = fromPairs([toString(id) → interestScore])
        Expression memberList = Cypher.listWith(x).in(friendData)
                .returning(Cypher.property(x, "member"));

        Expression pairList = Cypher.listWith(x).in(friendData)
                .returning(Cypher.listOf(
                        Cypher.call("toString")
                                .withArgs(Cypher.property(Cypher.property(x, "member"), PROP_ID))
                                .asFunction(),
                        Cypher.property(x, "interestScore")
                ));

        // me를 WITH 체인 끝까지 전달: CALL 서브쿼리에서 targetMember = me 엣지를 명시적으로 제외하기 위해
        StatementBuilder.OngoingReadingAndWith withMaps = withFriendData
                .with(
                        me,
                        friendData,
                        Cypher.call("apoc.coll.union")
                                .withArgs(memberList, Cypher.listOf(me.getRequiredSymbolicName()))
                                .asFunction().as("boundary"),
                        Cypher.call("apoc.map.fromPairs").withArgs(pairList).asFunction().as("interestMap")
                );

        // Step C: UNWIND friendData → 개별 member·myFriendship 복원 후 dynamicLimit 계산
        Expression myIntimacy = Cypher.coalesce(myFriendship.property(PROP_INTIMACY), Cypher.literalOf(0.0));
        Expression limitCalc = Cypher.call("toInteger").withArgs(
                Cypher.literalOf(5).add(myIntimacy.multiply(Cypher.literalOf(25)))
        ).asFunction();

        StatementBuilder.OngoingReadingAndWith withLimit = withMaps
                .unwind(friendData).as("item")
                .with(
                        me,
                        boundary,
                        interestMap,
                        Cypher.property(item, "member").as(member.getRequiredSymbolicName().getValue()),
                        Cypher.property(item, "friendship").as(myFriendship.getRequiredSymbolicName().getValue())
                )
                .with(me, boundary, interestMap, member, limitCalc.as("dynamicLimit"));

        // Step D: CALL 서브쿼리 — member 1명분 내부 엣지를 독립 실행으로 격리 (메모리 병목 해소)
        // collect()와 [0..dynamicLimit] 슬라이스를 WITH으로 분리해야 집계 implicit grouping 오류 회피
        // targetMember = me 조건: 기존 코드의 마지막 두 MATCH가 암묵적으로 걸렀던 me-to-friend 엣지를 명시적으로 제외
        SymbolicName allEdges = Cypher.name("allEdges");

        Expression edgeMap = Cypher.mapOf(
                "innerFriendship", innerFriendship.getRequiredSymbolicName(),
                "targetMember", targetMember.getRequiredSymbolicName()
        );

        Statement innerStatement = Cypher.with(me, member, boundary, dynamicLimit)
                .match(friendshipBetween(member, innerFriendship, targetMember))
                .where(targetMember.getRequiredSymbolicName().in(boundary)
                        .and(member.isNotEqualTo(targetMember))
                        .and(targetMember.isNotEqualTo(me)))
                .with(innerFriendship, targetMember, dynamicLimit)
                .orderBy(innerFriendship.property(PROP_INTIMACY).descending())
                .with(Cypher.collect(edgeMap).as("allEdges"), dynamicLimit)
                .returning(
                        Cypher.subList(allEdges, Cypher.literalOf(0), dynamicLimit).as("topEdges")
                )
                .build();

        // Step E: UNWIND topEdges → RETURN (interestMap 룩업으로 재탐색 제거)
        Expression friendAInterest = Cypher.valueAt(
                interestMap,
                Cypher.call("toString").withArgs(member.property(PROP_ID)).asFunction()
        );
        Expression friendBInterest = Cypher.valueAt(
                interestMap,
                Cypher.call("toString")
                        .withArgs(Cypher.property(Cypher.property(edgeData, "targetMember"), PROP_ID))
                        .asFunction()
        );

        return withLimit
                .call(innerStatement)
                .unwind(topEdges).as("edgeData")
                .returning(
                        member.property(PROP_ID).as("friendA_Id"),
                        Cypher.property(Cypher.property(edgeData, "targetMember"), PROP_ID).as("friendB_Id"),
                        Cypher.property(Cypher.property(edgeData, "innerFriendship"), PROP_INTIMACY).as("intimacy"),
                        friendAInterest.as("friendA_Interest"),
                        friendBInterest.as("friendB_Interest")
                )
                .build();
    }

    private static final String GET_NETWORK_CONTACTS_OF_TWO_HOP = """
            MATCH (me:UserReference {id: $meId})
            WITH me
            MATCH (target:UserReference {id: $targetId})
            CALL (me, target) {
              MATCH (me)-[:HAS_FRIENDSHIP]->(:Friendship)<-[:HAS_FRIENDSHIP]-(mutual:UserReference)
              WHERE mutual.id IN $skeletonIds
              MATCH (target)-[:HAS_FRIENDSHIP]->(tf:Friendship)<-[:HAS_FRIENDSHIP]-(mutual)
              ORDER BY tf.intimacy DESC
              LIMIT 5
              RETURN mutual, tf
            }
            RETURN mutual.id AS friendId
            """;

    private static final String GET_NEW_NODE_EDGES = """
            MATCH (me:UserReference {id: $meId})
            WITH me
            MATCH (target:UserReference {id: $targetId})
            CALL (me, target) {
              MATCH (me)-[:HAS_FRIENDSHIP]->(:Friendship)<-[:HAS_FRIENDSHIP]-(mutual:UserReference)
              WHERE mutual.id IN $skeletonIds
              MATCH (target)-[:HAS_FRIENDSHIP]->(tf:Friendship)<-[:HAS_FRIENDSHIP]-(mutual)
              ORDER BY tf.intimacy DESC
              LIMIT $dynamicLimit
              RETURN mutual, tf
            }
            RETURN target.id AS friendAId, mutual.id AS friendBId, tf.intimacy AS intimacy
            """;

    @Override
    public List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(
            Long userId, Long targetId, List<Long> skeletonIds) {

        return neo4jClient.query(GET_NETWORK_CONTACTS_OF_TWO_HOP)
                .bind(userId).to("meId")
                .bind(targetId).to("targetId")
                .bind(skeletonIds).to("skeletonIds")
                .fetchAs(NetworkOneHopsByTwoHopResult.class)
                .mappedBy((typeSystem, record) -> new NetworkOneHopsByTwoHopResult(
                        record.get("friendId").asLong()
                ))
                .all().stream().toList();
    }

    @Override
    public List<MutualFriendEdgeResult> getNewNodeEdges(
            Long userId, Long targetId, List<Long> skeletonIds, int dynamicLimit) {

        return neo4jClient.query(GET_NEW_NODE_EDGES)
                .bind(userId).to("meId")
                .bind(targetId).to("targetId")
                .bind(skeletonIds).to("skeletonIds")
                .bind(dynamicLimit).to("dynamicLimit")
                .fetchAs(MutualFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new MutualFriendEdgeResult(
                        record.get("friendAId").asLong(),
                        record.get("friendBId").asLong(),
                        record.get("intimacy").asDouble()
                ))
                .all().stream().toList();
    }
}
