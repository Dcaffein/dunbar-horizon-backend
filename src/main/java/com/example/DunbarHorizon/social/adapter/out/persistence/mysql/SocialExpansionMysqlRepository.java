package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.port.out.SocialExpansionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class SocialExpansionMysqlRepository implements SocialExpansionRepository {

    // Prevent merging candidates into the outer join so MySQL can sort before scoring and stop at LIMIT.
    private static final String ANCHOR_QUERY = """
            WITH my_friends AS (
                SELECT f.high_user_id AS id FROM friendships f
                JOIN users friend ON friend.user_id = f.high_user_id AND friend.status = 'ACTIVE'
                WHERE f.low_user_id = :meId AND f.high_user_id <> :anchorId
                UNION ALL
                SELECT f.low_user_id AS id FROM friendships f
                JOIN users friend ON friend.user_id = f.low_user_id AND friend.status = 'ACTIVE'
                WHERE f.high_user_id = :meId AND f.low_user_id <> :anchorId
            ), anchor_friends AS (
                SELECT high_user_id AS id, intimacy FROM friendships
                WHERE low_user_id = :anchorId AND high_user_id <> :meId AND high_is_routable
                UNION ALL
                SELECT low_user_id AS id, intimacy FROM friendships
                WHERE high_user_id = :anchorId AND low_user_id <> :meId AND low_is_routable
            ), shared_labels AS (
                SELECT DISTINCT l.id FROM labels l
                JOIN label_members m ON m.label_id = l.id AND m.member_id = :meId
                WHERE l.owner_id = :anchorId
            ), candidates AS (
                SELECT af.id, af.intimacy
                FROM anchor_friends af
                JOIN users candidate ON candidate.user_id = af.id AND candidate.status = 'ACTIVE'
                JOIN users me ON me.user_id = :meId AND me.status = 'ACTIVE'
                JOIN users anchor ON anchor.user_id = :anchorId AND anchor.status = 'ACTIVE'
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

    private static final String IN_MY_FRIENDS = "EXISTS (SELECT 1 FROM my_friends m WHERE m.id = af.id)";
    private static final String RELATED_QUERY = ANCHOR_QUERY.formatted(IN_MY_FRIENDS);
    private static final String RECOMMENDED_QUERY = ANCHOR_QUERY.formatted("NOT " + IN_MY_FRIENDS);

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<AnchorExpansionResult> getRecommendedNetworkByAnchor(Long meId, Long anchorId,
                                                                    int threshold, int limitCount) {
        return query(RECOMMENDED_QUERY, meId, anchorId, threshold, limitCount);
    }

    @Override
    public List<AnchorExpansionResult> getRelatedNetworkByAnchor(Long meId, Long anchorId,
                                                                int threshold, int limitCount) {
        return query(RELATED_QUERY, meId, anchorId, threshold, limitCount);
    }

    private List<AnchorExpansionResult> query(String sql, Long meId, Long anchorId, int threshold, int limitCount) {
        return jdbc.query(sql, Map.of("meId", meId, "anchorId", anchorId,
                        "threshold", threshold, "limitCount", limitCount),
                (row, index) -> new AnchorExpansionResult(row.getLong("id"), row.getString("nickname")));
    }
}
