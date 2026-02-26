package com.example.GooRoomBe.social.adapter.out.neo4j.dsl;

import org.neo4j.cypherdsl.core.*;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReading;

import static com.example.GooRoomBe.social.domain.friend.constant.FriendConstants.FRIENDSHIP;
import static com.example.GooRoomBe.social.domain.friend.constant.FriendConstants.MEMBER_OF;
import static com.example.GooRoomBe.social.adapter.out.neo4j.dsl.SocialNetworkVariables.*;
import static com.example.GooRoomBe.social.domain.label.constant.LabelConstants.HAS_MEMBER;
import static com.example.GooRoomBe.social.domain.label.constant.LabelConstants.LABEL;
import static com.example.GooRoomBe.social.domain.label.constant.LabelConstants.OWNS;
import static com.example.GooRoomBe.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public final class SocialNetworkPatterns {

    public static Node user() { return Cypher.node(USER_REFERENCE); }
    public static Node friendship() { return Cypher.node(FRIENDSHIP); }
    public static Node label() { return Cypher.node(LABEL); }

    public static OngoingReading matchUserWithId(Long userId, String alias) {
        Node user = user().named(alias);
        return Cypher.match(user).where(idEquals(user, userId));
    }

    /**
     * <p><b>Cypher:</b> {@code (userA)-[:MEMBER_OF]->(fs:FriendShip)<-[:MEMBER_OF]-(userB)}</p>
     */
    public static RelationshipChain friendshipBetween(Node userA, Node friendshipNode, Node userB) {
        return userA.relationshipTo(friendshipNode, MEMBER_OF)
                .relationshipFrom(userB, MEMBER_OF);
    }

    /**
     * <p><b>Cypher:</b> {@code (user)-[:OWNS]->(label)}</p>
     */
    public static Relationship ownsLabel(Node user, Node labelNode) {
        return labelNode.relationshipFrom(user, OWNS);
    }

    /**
     * <p><b>Cypher:</b> {@code (label)-[:HAS_MEMBER]->(user)}</p>
     */
    public static Relationship hasMember(Node labelNode, Node user) {
        return labelNode.relationshipTo(user, HAS_MEMBER);
    }


    /**
     * <p><b>Cypher:</b> {@code WHERE node.id = $id}</p>
     */
    public static Condition idEquals(Node node, Long id) {
        return node.property(PROP_ID).isEqualTo(Cypher.literalOf(id));
    }

    /**
     * <p><b>Cypher:</b> {@code WHERE nodeA.id = nodeB.id}</p>
     */
    public static Condition isSameNode(Node nodeA, Node nodeB) {
        return nodeA.property(PROP_ID).isEqualTo(nodeB.property(PROP_ID));
    }

    /**
     * <p><b>Cypher:</b> {@code WHERE nodeA.id <> nodeB.id}</p>
     */
    public static Condition isNotSameNode(Node nodeA, Node nodeB) {
        return nodeA.property(PROP_ID).isNotEqualTo(nodeB.property(PROP_ID));
    }
    /**
     * <p><b>Cypher:</b> {@code WHERE relationship.isRoutable = true}</p>
     */
    public static Condition isRoutable(Relationship memberOf) {
        return memberOf.property(PROP_IS_ROUTABLE).isTrue();
    }

    /**
     * <p><b>Cypher:</b> {@code WHERE label.exposure = true}</p>
     */
    public static Condition isLabelExposed(Node labelNode) {
        return labelNode.property(PROP_EXPOSURE).isTrue();
    }

    /**
    * <p><b>Cypher:</b> {@code WHERE target.id <> me.id AND NOT (target.id IN $exclusionIds)}</p>
     */
    public static Condition isNotSelfAndNotExcluded(Node target, Node me, SymbolicName exclusionIds) {
        return target.property(PROP_ID).isNotEqualTo(me.property(PROP_ID))
                .and(target.property(PROP_ID).in(exclusionIds).not());
    }

    private SocialNetworkPatterns() {}
}