package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NodeEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NodeGraphResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SocialNetworkMysqlRepository implements SocialNetworkRepository {

    private static final String NETWORK_GRAPH = """
            WITH my_friends AS (
                SELECT high_user_id AS friend_id, intimacy, low_interest_score AS raw_interest
                FROM friendships WHERE low_user_id = :userId
                UNION ALL
                SELECT low_user_id AS friend_id, intimacy, high_interest_score AS raw_interest
                FROM friendships WHERE high_user_id = :userId
            ), boundary AS (
                SELECT f.friend_id, f.intimacy, f.raw_interest
                FROM my_friends f
                JOIN users me ON me.user_id = :userId AND me.status = 'ACTIVE'
                JOIN users member ON member.user_id = f.friend_id AND member.status = 'ACTIVE'
                %s
                ORDER BY f.intimacy DESC, f.friend_id
                LIMIT :circleSize
            ), directed_edges AS (
                SELECT IF(direction.flipped = 0, a.friend_id, b.friend_id) AS node_id,
                       IF(direction.flipped = 0, b.friend_id, a.friend_id) AS other_id,
                       f.intimacy
                FROM boundary a
                STRAIGHT_JOIN boundary b ON a.friend_id < b.friend_id
                STRAIGHT_JOIN friendships f ON f.low_user_id = a.friend_id AND f.high_user_id = b.friend_id
                STRAIGHT_JOIN (SELECT 0 AS flipped UNION ALL SELECT 1) direction ON TRUE
            ), ranked_edges AS (
                SELECT node_id, other_id, intimacy,
                       ROW_NUMBER() OVER (PARTITION BY node_id ORDER BY intimacy DESC, other_id) AS edge_rank
                FROM directed_edges
            )
            SELECT b.friend_id, b.raw_interest, e.other_id, e.intimacy AS edge_intimacy
            FROM boundary b
            LEFT JOIN ranked_edges e ON e.node_id = b.friend_id
                AND e.edge_rank <= FLOOR(:pruningMin + b.intimacy * :pruningRange)
            ORDER BY b.intimacy DESC, b.friend_id, e.edge_rank
            """;

    private static final String DEFAULT_NETWORK = NETWORK_GRAPH.formatted("");
    private static final String LABEL_NETWORK = NETWORK_GRAPH.formatted("""
            JOIN label_members lm ON lm.member_id = f.friend_id AND lm.label_id = :labelId
            JOIN labels l ON l.id = lm.label_id AND l.owner_id = :userId
            """);

    private static final String TARGET_EDGES = """
            WITH base AS (
                SELECT high_user_id AS friend_id FROM friendships
                WHERE low_user_id = :userId AND high_user_id IN (:baseIds)
                UNION ALL
                SELECT low_user_id AS friend_id FROM friendships
                WHERE high_user_id = :userId AND low_user_id IN (:baseIds)
            )
            SELECT base.friend_id, edge.intimacy
            FROM base
            JOIN users me ON me.user_id = :userId AND me.status = 'ACTIVE'
            JOIN users target ON target.user_id = :targetId AND target.status = 'ACTIVE'
            JOIN users mutual ON mutual.user_id = base.friend_id AND mutual.status = 'ACTIVE'
            JOIN friendships edge ON edge.low_user_id = LEAST(:targetId, base.friend_id)
                                AND edge.high_user_id = GREATEST(:targetId, base.friend_id)
            ORDER BY edge.intimacy DESC, base.friend_id
            LIMIT :edgeLimit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<NodeGraphResult> getDefaultNetworkGraph(Long userId, DunbarCircle circleSize,
                                                       int pruningMin, int pruningRange) {
        return readGraph(DEFAULT_NETWORK, graphParameters(userId, circleSize, pruningMin, pruningRange));
    }

    @Override
    public List<NodeGraphResult> getLabelCustomNetwork(Long userId, String labelId, DunbarCircle circleSize,
                                                       int pruningMin, int pruningRange) {
        return readGraph(LABEL_NETWORK,
                graphParameters(userId, circleSize, pruningMin, pruningRange).addValue("labelId", labelId));
    }

    @Override
    public List<MutualFriendEdgeResult> getDirectFriendEdgesForTarget(
            Long userId, Long targetId, List<Long> baseNetworkFriendIds, int dynamicLimit) {
        return readTargetEdges(userId, targetId, baseNetworkFriendIds, dynamicLimit, true);
    }

    @Override
    public List<MutualFriendEdgeResult> getTwoHopContactEdgesForTarget(
            Long userId, Long targetId, List<Long> baseNetworkFriendIds, int strangerLimit) {
        return readTargetEdges(userId, targetId, baseNetworkFriendIds, strangerLimit, false);
    }

    private List<MutualFriendEdgeResult> readTargetEdges(Long userId, Long targetId, List<Long> baseIds,
                                                       int limit, boolean exposeIntimacy) {
        if (baseIds.isEmpty() || limit == 0) {
            return List.of();
        }
        return jdbc.query(TARGET_EDGES, Map.of("userId", userId, "targetId", targetId,
                        "baseIds", baseIds, "edgeLimit", limit),
                (row, index) -> new MutualFriendEdgeResult(targetId, row.getLong("friend_id"),
                        exposeIntimacy ? row.getDouble("intimacy") : null));
    }

    private static MapSqlParameterSource graphParameters(Long userId, DunbarCircle circleSize,
                                                        int pruningMin, int pruningRange) {
        return new MapSqlParameterSource("userId", userId)
                .addValue("circleSize", circleSize.getLimitSize())
                .addValue("pruningMin", pruningMin)
                .addValue("pruningRange", pruningRange);
    }

    private List<NodeGraphResult> readGraph(String sql, MapSqlParameterSource parameters) {
        Map<Long, NodeGraphResult> nodes = new LinkedHashMap<>();
        jdbc.query(sql, parameters, row -> {
            long nodeId = row.getLong("friend_id");
            NodeGraphResult node = nodes.get(nodeId);
            if (node == null) {
                node = new NodeGraphResult(nodeId, FriendRecognition.normalize(row.getDouble("raw_interest")),
                        new ArrayList<>());
                nodes.put(nodeId, node);
            }
            Long otherId = row.getObject("other_id", Long.class);
            if (otherId != null) {
                node.edges().add(new NodeEdgeResult(otherId, row.getDouble("edge_intimacy")));
            }
        });
        return nodes.values().stream()
                .map(node -> new NodeGraphResult(node.nodeId(), node.interestScore(), List.copyOf(node.edges())))
                .toList();
    }
}
