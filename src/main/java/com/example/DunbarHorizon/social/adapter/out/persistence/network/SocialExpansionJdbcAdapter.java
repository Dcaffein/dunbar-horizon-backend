package com.example.DunbarHorizon.social.adapter.out.persistence.network;

import com.example.DunbarHorizon.social.application.network.dto.IntermediaryExpansionResult;
import com.example.DunbarHorizon.social.application.network.port.SocialExpansionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SocialExpansionJdbcAdapter implements SocialExpansionRepository {

    // Prevent merging candidates into the outer join so MySQL can sort before scoring and stop at LIMIT.
    private static final String SEED_FRIENDS_QUERY = """
            WITH my_friends AS (
                SELECT f.high_user_id AS id FROM friendships f
                JOIN users friend ON friend.user_id = f.high_user_id AND friend.status = 'ACTIVE'
                WHERE f.low_user_id = :meId AND f.high_user_id <> :seedId
                UNION ALL
                SELECT f.low_user_id AS id FROM friendships f
                JOIN users friend ON friend.user_id = f.low_user_id AND friend.status = 'ACTIVE'
                WHERE f.high_user_id = :meId AND f.low_user_id <> :seedId
            ), seed_friends AS (
                SELECT high_user_id AS id, intimacy FROM friendships
                WHERE low_user_id = :seedId AND high_user_id <> :meId AND high_is_routable
                UNION ALL
                SELECT low_user_id AS id, intimacy FROM friendships
                WHERE high_user_id = :seedId AND low_user_id <> :meId AND low_is_routable
            ), shared_labels AS (
                SELECT DISTINCT l.id FROM labels l
                JOIN label_members m ON m.label_id = l.id AND m.member_id = :meId
                WHERE l.owner_id = :seedId
            ), candidates AS (
                SELECT sf.id, sf.intimacy
                FROM seed_friends sf
                JOIN users candidate ON candidate.user_id = sf.id AND candidate.status = 'ACTIVE'
                JOIN users me ON me.user_id = :meId AND me.status = 'ACTIVE'
                JOIN users seed ON seed.user_id = :seedId AND seed.status = 'ACTIVE'
                WHERE EXISTS (SELECT 1 FROM my_friends)
                  AND %s
                LIMIT 18446744073709551615
            )
            SELECT c.id, u.nickname FROM candidates c
            STRAIGHT_JOIN LATERAL (
                SELECT 1 AS qualified
                WHERE (
                    (SELECT COUNT(*) FROM friendships f FORCE INDEX (PRIMARY)
                     STRAIGHT_JOIN my_friends m ON m.id = f.high_user_id WHERE f.low_user_id = c.id)
                    +
                    (SELECT COUNT(*) FROM friendships f FORCE INDEX (idx_friendships_high_low)
                     STRAIGHT_JOIN my_friends m ON m.id = f.low_user_id WHERE f.high_user_id = c.id)
                    +
                    (SELECT COUNT(*) FROM shared_labels l
                     JOIN label_members m ON m.label_id = l.id AND m.member_id = c.id)
                ) >= :threshold
                LIMIT 1
            ) qualifying ON TRUE
            STRAIGHT_JOIN users u ON u.user_id = c.id
            ORDER BY c.intimacy DESC, c.id
            LIMIT :limitCount
            """;

    private static final String IN_MY_FRIENDS = "EXISTS (SELECT 1 FROM my_friends m WHERE m.id = sf.id)";
    private static final String RELATED_QUERY = SEED_FRIENDS_QUERY.formatted(IN_MY_FRIENDS);
    private static final String RECOMMENDED_QUERY = SEED_FRIENDS_QUERY.formatted("NOT " + IN_MY_FRIENDS);

    private static final String INTIMACY = """
            SELECT f.intimacy
            FROM friendships f
            JOIN users low_user ON low_user.user_id = f.low_user_id AND low_user.status = 'ACTIVE'
            JOIN users high_user ON high_user.user_id = f.high_user_id AND high_user.status = 'ACTIVE'
            WHERE f.low_user_id = LEAST(:userId, :friendId) AND f.high_user_id = GREATEST(:userId, :friendId)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<IntermediaryExpansionResult> getRecommendedNetworkByIntermediary(Long meId, Long seedId,
                                                                    int threshold, int limitCount) {
        return query(RECOMMENDED_QUERY, meId, seedId, threshold, limitCount);
    }

    @Override
    public List<IntermediaryExpansionResult> getRelatedNetworkByIntermediary(Long meId, Long seedId,
                                                                int threshold, int limitCount) {
        return query(RELATED_QUERY, meId, seedId, threshold, limitCount);
    }

    private List<IntermediaryExpansionResult> query(String sql, Long meId, Long seedId, int threshold, int limitCount) {
        return jdbc.query(sql, Map.of("meId", meId, "seedId", seedId,
                        "threshold", threshold, "limitCount", limitCount),
                (row, index) -> new IntermediaryExpansionResult(row.getLong("id"), row.getString("nickname")));
    }

    @Override
    public Optional<Double> findIntimacy(Long userId, Long friendId) {
        return jdbc.queryForList(INTIMACY, Map.of("userId", userId, "friendId", friendId), Double.class)
                .stream().findFirst();
    }
}
