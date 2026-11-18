package com.example.DunbarHorizon.social.adapter.out.persistence.jdbc;

import com.example.DunbarHorizon.social.adapter.out.persistence.FriendshipKey;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import static com.example.DunbarHorizon.social.adapter.out.persistence.FriendshipConditions.ACTIVE_PAIR;

@Repository
@RequiredArgsConstructor
public class FriendshipJdbcRepository {

    private static final String SELECT_PAIR_FOR_UPDATE = """
            SELECT * FROM friendships
            WHERE low_user_id = :low AND high_user_id = :high
              AND %s
            FOR UPDATE
            """.formatted(ACTIVE_PAIR);

    private static final String LOCK_PAIRS = """
            SELECT * FROM friendships FORCE INDEX (PRIMARY)
            WHERE (low_user_id, high_user_id) IN (:pairs)
              AND %s
            ORDER BY low_user_id, high_user_id
            FOR UPDATE
            """.formatted(ACTIVE_PAIR);

    private static final String SELECT_FRIEND_IDS_BY_MUTE_STATUS = """
            SELECT high_user_id FROM friendships
            WHERE low_user_id = :userId AND low_is_muted = :muted AND %1$s
            UNION ALL
            SELECT low_user_id FROM friendships
            WHERE high_user_id = :userId AND high_is_muted = :muted AND %1$s
            """.formatted(ACTIVE_PAIR);

    private static final String SELECT_FRIEND_IDS_AMONG = """
            SELECT high_user_id FROM friendships
            WHERE low_user_id = :userId AND high_user_id IN (:candidateIds) AND %1$s
            UNION ALL
            SELECT low_user_id FROM friendships
            WHERE high_user_id = :userId AND low_user_id IN (:candidateIds) AND %1$s
            """.formatted(ACTIVE_PAIR);

    private static final String APPLY_DECAY = """
            UPDATE friendships FORCE INDEX (PRIMARY)
            SET low_interest_score = CASE
                    WHEN low_last_interacted_at <= :decayTime AND low_interest_score > :threshold
                    THEN GREATEST(low_interest_score * :rate, :threshold) ELSE low_interest_score END,
                high_interest_score = CASE
                    WHEN high_last_interacted_at <= :decayTime AND high_interest_score > :threshold
                    THEN GREATEST(high_interest_score * :rate, :threshold) ELSE high_interest_score END
            WHERE (low_last_interacted_at <= :decayTime OR high_last_interacted_at <= :decayTime)
              AND %s
            ORDER BY low_user_id, high_user_id
            """.formatted(ACTIVE_PAIR);

    private final NamedParameterJdbcTemplate jdbc;

    public Optional<Friendship> findForUpdate(FriendshipKey key) {
        return jdbc.query(SELECT_PAIR_FOR_UPDATE, parameters(key), FriendshipRows::read).stream().findFirst();
    }

    public List<Friendship> findAllForUpdate(Collection<FriendshipKey> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        List<Object[]> pairs = keys.stream().map(key -> new Object[]{key.low(), key.high()}).toList();
        return jdbc.query(LOCK_PAIRS, Map.of("pairs", pairs), FriendshipRows::read);
    }

    public Set<Long> findFriendIdsByMuteStatus(long userId, boolean isMuted) {
        return new HashSet<>(jdbc.queryForList(SELECT_FRIEND_IDS_BY_MUTE_STATUS,
                Map.of("userId", userId, "muted", isMuted), Long.class));
    }

    public Set<Long> filterFriendIdsAmong(long userId, Collection<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList(SELECT_FRIEND_IDS_AMONG,
                Map.of("userId", userId, "candidateIds", candidateIds), Long.class));
    }

    public void saveInterestScores(Collection<Friendship> friendships) {
        if (friendships.isEmpty()) {
            return;
        }
        var parameters = new MapSqlParameterSource();
        var rows = new StringJoiner(", ");
        int index = 0;
        for (Friendship friendship : friendships) {
            FriendRecognition low = friendship.getLower();
            FriendRecognition high = friendship.getHigher();
            String suffix = Integer.toString(index++);
            rows.add("""
                    ROW(:low%1$s, :high%1$s, :lowScore%1$s, :highScore%1$s,
                        CAST(:lowTime%1$s AS DATETIME(6)), CAST(:highTime%1$s AS DATETIME(6)))
                    """.formatted(suffix));
            parameters.addValue("low" + suffix, low.getOwnerId())
                    .addValue("high" + suffix, high.getOwnerId())
                    .addValue("lowScore" + suffix, low.getInterestScore())
                    .addValue("highScore" + suffix, high.getInterestScore())
                    .addValue("lowTime" + suffix, low.getLastInteractedAt(), Types.TIMESTAMP)
                    .addValue("highTime" + suffix, high.getLastInteractedAt(), Types.TIMESTAMP);
        }
        jdbc.update("""
                UPDATE friendships f
                JOIN (VALUES %s) AS scores (low_id, high_id, low_score, high_score, low_time, high_time)
                  ON f.low_user_id = scores.low_id AND f.high_user_id = scores.high_id
                SET f.low_interest_score = scores.low_score,
                    f.high_interest_score = scores.high_score,
                    f.low_last_interacted_at = scores.low_time,
                    f.high_last_interacted_at = scores.high_time
                """.formatted(rows), parameters);
    }

    public void applyDecay(double rate, double threshold, LocalDateTime decayTime) {
        jdbc.update(APPLY_DECAY, Map.of("rate", rate, "threshold", threshold, "decayTime", decayTime));
    }

    private static Map<String, Long> parameters(FriendshipKey key) {
        return Map.of("low", key.low(), "high", key.high());
    }
}
