package com.example.DunbarHorizon.social.adapter.out.persistence.network;

import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult.Intermediaries;
import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult.IntermediaryResult;
import com.example.DunbarHorizon.social.application.port.out.SocialConnectionPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SocialConnectionPathJdbcRepository implements SocialConnectionPathRepository {

    private static final String INTERMEDIARIES = """
            WITH my_friends AS (
                SELECT high_user_id AS id, intimacy FROM friendships
                WHERE low_user_id = :myId AND high_is_routable
                UNION ALL
                SELECT low_user_id AS id, intimacy FROM friendships
                WHERE high_user_id = :myId AND low_is_routable
            ), target_friends AS (
                SELECT high_user_id AS id, intimacy FROM friendships
                WHERE low_user_id = :targetId AND low_is_routable
                UNION ALL
                SELECT low_user_id AS id, intimacy FROM friendships
                WHERE high_user_id = :targetId AND high_is_routable
            )
            SELECT mid.user_id, mid.nickname, COUNT(*) OVER () AS total_count
            FROM my_friends f1
            JOIN target_friends f2 ON f2.id = f1.id
            JOIN users mid ON mid.user_id = f1.id AND mid.status = 'ACTIVE'
            JOIN users me ON me.user_id = :myId AND me.status = 'ACTIVE'
            JOIN users target ON target.user_id = :targetId AND target.status = 'ACTIVE'
            WHERE me.user_id <> target.user_id
            ORDER BY f1.intimacy * f2.intimacy DESC, mid.user_id
            LIMIT :rowLimit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Intermediaries findIntermediaries(Long myId, Long targetId, int limit) {
        // The first row carries the full count even when the caller requests zero items.
        return jdbc.query(INTERMEDIARIES, Map.of("myId", myId, "targetId", targetId, "rowLimit", Math.max(1, limit)),
                rows -> {
                    if (!rows.next()) {
                        return new Intermediaries(List.of(), 0);
                    }
                    int totalCount = rows.getInt("total_count");
                    if (limit <= 0) {
                        return new Intermediaries(List.of(), totalCount);
                    }
                    var items = new ArrayList<IntermediaryResult>();
                    do {
                        items.add(new IntermediaryResult(rows.getLong("user_id"), rows.getString("nickname")));
                    } while (rows.next());
                    return new Intermediaries(List.copyOf(items), totalCount);
                });
    }
}
