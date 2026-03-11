package com.example.DunbarHorizon.social.adapter.out.neo4j.dsl;

import org.neo4j.cypherdsl.core.*;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReading;

import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.FRIENDSHIP;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.HAS_FRIENDSHIP;
import static com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.SocialNetworkProperties.*;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.ATTACHED_TO;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.LABEL;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.OWNS_LABEL;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public final class SocialNetworkPatterns {

    public static Node user() { return Cypher.node(USER_REFERENCE); }
    public static Node friendship() { return Cypher.node(FRIENDSHIP); }
    public static Node label() { return Cypher.node(LABEL); }

    public static OngoingReading matchUserWithId(Long userId, String alias) {
        Node user = user().named(alias);
        return Cypher.match(user).where(idEquals(user, userId));
    }

    /**
     * <p><b>Cypher:</b> {@code (userA)-[:HAS_FRIENDSHIP]->(fs:FriendShip)<-[:HAS_FRIENDSHIP]-(userB)}</p>
     */
    public static RelationshipChain friendshipBetween(Node userA, Node friendshipNode, Node userB) {
        return userA.relationshipTo(friendshipNode, HAS_FRIENDSHIP)
                .relationshipFrom(userB, HAS_FRIENDSHIP);
    }

    /**
     * <p><b>Cypher:</b> {@code (user)-[:OWNS_LABEL]->(label)}</p>
     */
    public static Relationship ownsLabel(Node user, Node labelNode) {
        return user.relationshipTo(labelNode, OWNS_LABEL);
    }

    /**
     * <p><b>Cypher:</b> {@code (label)-[:ATTACHED_TO]->(user)}</p>
     */
    public static Relationship ATTACHED_TO_LABEL(Node user, Node labelNode) {
        return user.relationshipTo(labelNode, ATTACHED_TO);
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