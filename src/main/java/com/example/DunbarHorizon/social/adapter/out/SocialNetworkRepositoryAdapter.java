package com.example.DunbarHorizon.social.adapter.out;

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

import static com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.SocialNetworkPatterns.*;
import static com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.SocialNetworkProperties.*;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.HAS_FRIENDSHIP;

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

        StatementBuilder.OngoingReading baseBuilder = matchUserWithId(userId, "me")
                .match(friendshipBetween(me, myFriendship, member));

        Statement statement = buildDynamicPruningNetwork(baseBuilder, me, member, myFriendship);

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

        StatementBuilder.OngoingReading baseBuilder = matchUserWithId(userId, "me")
                .match(me.relationshipTo(label, "HAS_LABEL").relationshipTo(member, "HAS_MEMBER"))
                .where(label.property("id").isEqualTo(Cypher.parameter("labelId")))
                .match(friendshipBetween(me, myFriendship, member));

        Statement statement = buildDynamicPruningNetwork(baseBuilder, me, member, myFriendship);

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
                .unwind(topEdges).as("edgeData")
                .with(
                        me, member, edgeData,
                        Cypher.property(edgeData, "targetMember").as(targetNode.getRequiredSymbolicName().getValue())
                )
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

    @Override
    public List<NetworkOneHopsByTwoHopResult> getIntersectionOneHops(
            Long userId, Long targetId, String labelId, int limitSize) {

        Node targetUser = user().named("targetUser");
        Node mutual = user().named("mutual");
        Node targetFriendship = friendship().named("targetFriendship");
        Relationship targetRelationship = targetUser.relationshipTo(targetFriendship, HAS_FRIENDSHIP).named("targetRelationship");
        SymbolicName currentSkeleton = Cypher.name("currentSkeleton");

        StatementBuilder.OngoingReadingAndWith withSkeleton = buildCurrentSkeleton(userId, labelId);

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
                .bind(labelId == null ? "" : labelId).to("labelId")
                .fetchAs(NetworkOneHopsByTwoHopResult.class)
                .mappedBy((typeSystem, record) -> new NetworkOneHopsByTwoHopResult(
                        record.get("friendId").asLong()
                ))
                .all().stream().toList();
    }

    @Override
    public List<MutualFriendEdgeResult> getIntersectionByOneHop(
            Long userId, Long targetId, String labelId, int limitSize) {

        Node target = user().named("target");
        Node mutual = user().named("mutual");
        Node targetFriendship = friendship().named("targetFriendship");
        Relationship targetRelationship = target.relationshipTo(targetFriendship, HAS_FRIENDSHIP).named("targetRelationship");
        SymbolicName currentSkeleton = Cypher.name("currentSkeleton");

        StatementBuilder.OngoingReadingAndWith withSkeleton = buildCurrentSkeleton(userId, labelId);

        Statement statement = withSkeleton
                .match(target).where(idEquals(target, targetId))
                .match(targetRelationship.relationshipFrom(mutual, HAS_FRIENDSHIP))
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
                .bind(labelId == null ? "" : labelId).to("labelId")
                .fetchAs(MutualFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new MutualFriendEdgeResult(
                        record.get("friendAId").asLong(),
                        record.get("friendBId").asLong(),
                        record.get("intimacy").asDouble()
                ))
                .all().stream().toList();
    }

    private StatementBuilder.OngoingReadingAndWith buildCurrentSkeleton(Long userId, String labelId) {
        Node me = user().named("me");
        Node myFriend = user().named("myFriend");
        Node myFriendship = friendship().named("myFriendship");
        StatementBuilder.OngoingReading builder = matchUserWithId(userId, "me")
                .match(friendshipBetween(me, myFriendship, myFriend));

        if (labelId != null && !labelId.isBlank()) {
            Node label = label().named("label");
            builder = builder.match(me.relationshipTo(label, "HAS_LABEL").relationshipTo(myFriend, "HAS_MEMBER"))
                    .where(label.property("id").isEqualTo(Cypher.parameter("labelId")));
        }

        return builder.with(myFriend, myFriendship)
                .orderBy(myFriendship.property(PROP_INTIMACY).descending())
                .limit(Cypher.parameter("limitSize"))
                .with(Cypher.collect(myFriend).as("currentSkeleton"));
    }
}
