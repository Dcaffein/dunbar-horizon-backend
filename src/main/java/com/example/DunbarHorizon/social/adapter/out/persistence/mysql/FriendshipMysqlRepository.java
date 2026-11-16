package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.application.port.out.FriendshipIdRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipLockRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipScoreRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

@Repository
@RequiredArgsConstructor
public class FriendshipMysqlRepository
        implements FriendshipRepository, FriendshipScoreRepository, FriendshipLockRepository, FriendshipIdRepository {

    private static final String ACTIVE_PAIR = """
            EXISTS (SELECT 1 FROM users WHERE user_id = low_user_id AND status = 'ACTIVE')
              AND EXISTS (SELECT 1 FROM users WHERE user_id = high_user_id AND status = 'ACTIVE')""";

    private static final String INSERT = """
            INSERT INTO friendships (
                low_user_id, high_user_id, created_at,
                low_friend_alias, low_interest_score, low_last_interacted_at, low_is_muted, low_is_routable,
                high_friend_alias, high_interest_score, high_last_interacted_at, high_is_muted, high_is_routable
            ) VALUES (
                :low, :high, :createdAt,
                :lowAlias, :lowScore, :lowInteractedAt, :lowMuted, :lowRoutable,
                :highAlias, :highScore, :highInteractedAt, :highMuted, :highRoutable
            )
            """;

    private static final String SELECT_PAIR = """
            SELECT * FROM friendships
            WHERE low_user_id = :low AND high_user_id = :high
              AND %s
            """.formatted(ACTIVE_PAIR);

    private static final String EXISTS_PAIR = """
            SELECT EXISTS (
                SELECT 1 FROM friendships
                WHERE low_user_id = :low AND high_user_id = :high
                  AND %s
            )
            """.formatted(ACTIVE_PAIR);

    private static final String LOCK_PAIR = """
            SELECT low_user_id FROM friendships
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

    private static final String SELECT_BY_USER = """
            SELECT * FROM friendships
            WHERE low_user_id = :userId AND %1$s
            UNION ALL
            SELECT * FROM friendships
            WHERE high_user_id = :userId AND %1$s
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

    private static final String UPDATE_SETTINGS = """
            UPDATE friendships
            SET {side}_friend_alias = :alias, {side}_is_muted = :muted, {side}_is_routable = :routable
            WHERE low_user_id = :low AND high_user_id = :high
              AND %s
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

    private static final String DELETE_PAIR = """
            DELETE FROM friendships
            WHERE low_user_id = :low AND high_user_id = :high
              AND %s
            """.formatted(ACTIVE_PAIR);

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void create(Friendship friendship) {
        try {
            jdbc.update(INSERT, FriendshipRows.insertParameters(friendship));
        } catch (DuplicateKeyException e) {
            throw new AlreadyFriendsException(friendship.getLower().getOwnerId(), friendship.getHigher().getOwnerId());
        }
    }

    @Override
    public Optional<Friendship> findBetween(Long userId, Long friendId) {
        return findPair(SELECT_PAIR, userId, friendId);
    }

    @Override
    public Optional<Friendship> findBetweenForUpdate(Long userId, Long friendId) {
        return findPair(SELECT_PAIR + "FOR UPDATE", userId, friendId);
    }

    @Override
    public List<Friendship> findAllBetweenForUpdate(Long userId, Collection<Long> friendIds) {
        return lockPairs(friendIds.stream().map(friendId -> UserPairKey.of(userId, friendId)).toList());
    }

    @Override
    public List<Friendship> findAllAmongForUpdate(Collection<Long> userIds) {
        List<Long> members = userIds.stream().filter(Objects::nonNull).distinct().toList();
        List<UserPairKey> pairs = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                pairs.add(UserPairKey.of(members.get(i), members.get(j)));
            }
        }
        return lockPairs(pairs);
    }

    @Override
    public boolean existsFriendshipBetween(Long userId, Long friendId) {
        var key = UserPairKey.of(userId, friendId);
        return Boolean.TRUE.equals(jdbc.queryForObject(EXISTS_PAIR, key.parameters(), Boolean.class));
    }

    @Override
    public boolean existsFriendshipForUpdate(Long userId, Long friendId) {
        var key = UserPairKey.of(userId, friendId);
        return !jdbc.queryForList(LOCK_PAIR, key.parameters(), Long.class).isEmpty();
    }

    @Override
    public List<Friendship> findByUserId(Long userId) {
        return jdbc.query(SELECT_BY_USER, Map.of("userId", userId), FriendshipRows::read);
    }

    @Override
    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted) {
        return new HashSet<>(jdbc.queryForList(SELECT_FRIEND_IDS_BY_MUTE_STATUS,
                Map.of("userId", userId, "muted", isMuted), Long.class));
    }

    @Override
    public Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList(SELECT_FRIEND_IDS_AMONG,
                Map.of("userId", userId, "candidateIds", candidateIds), Long.class));
    }

    @Override
    public void saveSettings(Friendship friendship, Long ownerId) {
        Long friendId = friendship.getFriendId(ownerId);
        FriendRecognition low = friendship.getLower();
        String side = low.getOwnerId().equals(ownerId) ? "low" : "high";
        var parameters = new MapSqlParameterSource()
                .addValue("low", low.getOwnerId())
                .addValue("high", friendship.getHigher().getOwnerId())
                .addValue("alias", FriendshipRows.storedAlias(friendship.getFriendAlias(ownerId)), Types.VARCHAR)
                .addValue("muted", friendship.isMuted(ownerId))
                .addValue("routable", friendship.isRoutable(ownerId));
        if (jdbc.update(UPDATE_SETTINGS.replace("{side}", side), parameters) == 0) {
            throw new FriendshipNotFoundException(ownerId, friendId);
        }
    }

    @Override
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

    @Override
    public void applyDecay(double rate, double threshold, LocalDateTime decayTime) {
        jdbc.update(APPLY_DECAY, Map.of("rate", rate, "threshold", threshold, "decayTime", decayTime));
    }

    @Override
    public boolean deleteBetween(Long userId, Long friendId) {
        var key = UserPairKey.of(userId, friendId);
        return jdbc.update(DELETE_PAIR, key.parameters()) > 0;
    }

    private Optional<Friendship> findPair(String sql, Long userId, Long friendId) {
        var key = UserPairKey.of(userId, friendId);
        return jdbc.query(sql, key.parameters(), FriendshipRows::read).stream().findFirst();
    }

    private List<Friendship> lockPairs(Collection<UserPairKey> keys) {
        List<Object[]> pairs = keys.stream()
                .filter(pair -> pair.low() < pair.high())
                .distinct()
                .map(pair -> new Object[]{pair.low(), pair.high()})
                .toList();
        if (pairs.isEmpty()) {
            return List.of();
        }
        return jdbc.query(LOCK_PAIRS, Map.of("pairs", pairs), FriendshipRows::read);
    }
}
