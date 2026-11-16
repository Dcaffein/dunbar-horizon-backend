package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class FriendshipMysqlRepository implements FriendshipRepository {

    private static final String PAIR = "low_user_id = :low AND high_user_id = :high";
    private static final String SELECT_PAIR = "SELECT * FROM friendships WHERE " + PAIR;
    private static final String INSERT = """
            INSERT INTO friendships (
                low_user_id, high_user_id, created_at, intimacy,
                low_friend_alias, low_interest_score, low_last_interacted_at, low_is_muted, low_is_routable,
                high_friend_alias, high_interest_score, high_last_interacted_at, high_is_muted, high_is_routable
            ) VALUES (
                :low, :high, :createdAt, :intimacy,
                :lowAlias, :lowScore, :lowInteractedAt, :lowMuted, :lowRoutable,
                :highAlias, :highScore, :highInteractedAt, :highMuted, :highRoutable
            )
            """;
    private static final String SELECT_FRIENDSHIPS = """
            SELECT * FROM friendships WHERE low_user_id = :userId
            UNION ALL
            SELECT * FROM friendships WHERE high_user_id = :userId
            """;
    private static final String SELECT_BY_MUTE_STATUS = """
            SELECT high_user_id FROM friendships WHERE low_user_id = :userId AND low_is_muted = :muted
            UNION ALL
            SELECT low_user_id FROM friendships WHERE high_user_id = :userId AND high_is_muted = :muted
            """;
    private static final String SELECT_FRIEND_IDS_AMONG = """
            SELECT high_user_id FROM friendships WHERE low_user_id = :userId AND high_user_id IN (:candidateIds)
            UNION ALL
            SELECT low_user_id FROM friendships WHERE high_user_id = :userId AND low_user_id IN (:candidateIds)
            """;
    private static final String ACTIVE_PAIR = """
            EXISTS (SELECT 1 FROM users WHERE user_id = low_user_id AND status = 'ACTIVE')
            AND EXISTS (SELECT 1 FROM users WHERE user_id = high_user_id AND status = 'ACTIVE')
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final SocialUserMysqlRepository users;

    @Override
    public void create(Friendship friendship) {
        FriendRecognition low = friendship.getLower();
        FriendRecognition high = friendship.getHigher();
        var parameters = new MapSqlParameterSource()
                .addValue("low", low.getOwnerId())
                .addValue("high", high.getOwnerId())
                .addValue("createdAt", friendship.getCreatedAt(), Types.DATE)
                .addValue("intimacy", friendship.getIntimacy());
        bindRecognition(parameters, "low", low);
        bindRecognition(parameters, "high", high);
        try {
            jdbc.update(INSERT, parameters);
        } catch (DuplicateKeyException e) {
            throw new AlreadyFriendsException(low.getOwnerId(), high.getOwnerId());
        }
    }

    @Override
    public Optional<Friendship> findBetween(Long userId, Long friendId) {
        return findPair(SELECT_PAIR, userId, friendId);
    }

    @Override
    public Optional<Friendship> findBetweenForUpdate(Long userId, Long friendId) {
        return findPair(SELECT_PAIR + " FOR UPDATE", userId, friendId);
    }

    private Optional<Friendship> findPair(String sql, Long userId, Long friendId) {
        var key = UserPairKey.of(userId, friendId);
        return restore(jdbc.query(sql, key.parameters(), FriendshipRow::read)).stream().findFirst();
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
    public List<Friendship> findArchivableForUpdate(double intimacyThreshold) {
        List<UserPairKey> candidates = jdbc.query("""
                SELECT low_user_id, high_user_id FROM friendships
                WHERE intimacy <= :threshold AND
                """ + ACTIVE_PAIR, Map.of("threshold", intimacyThreshold),
                (row, index) -> new UserPairKey(row.getLong("low_user_id"), row.getLong("high_user_id")));
        return lockPairs(candidates);
    }

    @Override
    public boolean existsFriendshipBetween(Long userId, Long targetId) {
        var key = UserPairKey.of(userId, targetId);
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM friendships WHERE " + PAIR + ")", key.parameters(), Boolean.class));
    }

    @Override
    public boolean existsFriendshipForUpdate(Long userId, Long targetId) {
        var key = UserPairKey.of(userId, targetId);
        return !jdbc.queryForList("SELECT low_user_id FROM friendships WHERE " + PAIR + " FOR UPDATE",
                key.parameters(), Long.class).isEmpty();
    }

    @Override
    public List<Friendship> findByUserId(Long userId) {
        return restore(jdbc.query(SELECT_FRIENDSHIPS, Map.of("userId", userId), FriendshipRow::read));
    }

    @Override
    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted) {
        List<Long> ids = jdbc.queryForList(SELECT_BY_MUTE_STATUS, Map.of("userId", userId, "muted", isMuted), Long.class);
        return activeFriendIds(userId, ids);
    }

    @Override
    public Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = jdbc.queryForList(SELECT_FRIEND_IDS_AMONG,
                Map.of("userId", userId, "candidateIds", candidateIds), Long.class);
        return activeFriendIds(userId, ids);
    }

    @Override
    public void updateUserFields(Friendship friendship, Long userId) {
        Long friendId = friendship.getFriendId(userId);
        String side = userId < friendId ? "low" : "high";
        var parameters = new MapSqlParameterSource(UserPairKey.of(userId, friendId).parameters())
                .addValue("alias", storedAlias(friendship.getFriendAlias(userId)), Types.VARCHAR)
                .addValue("muted", friendship.isMuted(userId))
                .addValue("routable", friendship.isRoutable(userId));
        int updated = jdbc.update("""
                UPDATE friendships
                SET %1$s_friend_alias = :alias, %1$s_is_muted = :muted, %1$s_is_routable = :routable
                WHERE %2$s AND %3$s
                """.formatted(side, PAIR, ACTIVE_PAIR), parameters);
        if (updated == 0) {
            throw new FriendshipNotFoundException(userId, friendId);
        }
    }

    private List<Friendship> lockPairs(Collection<UserPairKey> keys) {
        List<Object[]> pairs = pairParameters(keys);
        if (pairs.isEmpty()) {
            return List.of();
        }
        return restore(jdbc.query("""
                SELECT * FROM friendships FORCE INDEX (PRIMARY)
                WHERE (low_user_id, high_user_id) IN (:pairs)
                ORDER BY low_user_id, high_user_id
                FOR UPDATE
                """, Map.of("pairs", pairs), FriendshipRow::read));
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
                        CAST(:lowTime%1$s AS DATETIME(6)), CAST(:highTime%1$s AS DATETIME(6)), :intimacy%1$s)
                    """.formatted(suffix));
            parameters.addValue("low" + suffix, low.getOwnerId())
                    .addValue("high" + suffix, high.getOwnerId())
                    .addValue("lowScore" + suffix, low.getInterestScore())
                    .addValue("highScore" + suffix, high.getInterestScore())
                    .addValue("lowTime" + suffix, low.getLastInteractedAt(), Types.TIMESTAMP)
                    .addValue("highTime" + suffix, high.getLastInteractedAt(), Types.TIMESTAMP)
                    .addValue("intimacy" + suffix, friendship.getIntimacy());
        }
        jdbc.update("""
                UPDATE friendships f
                JOIN (VALUES %s) AS scores (low_id, high_id, low_score, high_score, low_time, high_time, intimacy)
                  ON f.low_user_id = scores.low_id AND f.high_user_id = scores.high_id
                SET f.low_interest_score = scores.low_score,
                    f.high_interest_score = scores.high_score,
                    f.low_last_interacted_at = scores.low_time,
                    f.high_last_interacted_at = scores.high_time,
                    f.intimacy = scores.intimacy
                """.formatted(rows), parameters);
    }

    @Override
    public void applyDecay(double rate, double threshold, LocalDateTime decayTime) {
        // In a single-table MySQL UPDATE, intimacy reads the scores assigned above it.
        jdbc.update("""
                UPDATE friendships FORCE INDEX (PRIMARY)
                SET low_interest_score = CASE
                        WHEN low_last_interacted_at <= :decayTime AND low_interest_score > :threshold
                        THEN GREATEST(low_interest_score * :rate, :threshold) ELSE low_interest_score END,
                    high_interest_score = CASE
                        WHEN high_last_interacted_at <= :decayTime AND high_interest_score > :threshold
                        THEN GREATEST(high_interest_score * :rate, :threshold) ELSE high_interest_score END,
                    intimacy = SQRT((low_interest_score / (low_interest_score + :convergence))
                                  * (high_interest_score / (high_interest_score + :convergence)))
                WHERE (low_last_interacted_at <= :decayTime OR high_last_interacted_at <= :decayTime)
                  AND
                """ + ACTIVE_PAIR + " ORDER BY low_user_id, high_user_id",
                Map.of("rate", rate, "threshold", threshold, "decayTime", decayTime,
                        "convergence", FriendRecognition.CONVERGENCE_K));
    }

    @Override
    public void deleteAll(Collection<Friendship> friendships) {
        List<Object[]> pairs = pairParameters(friendships.stream()
                .map(friendship -> new UserPairKey(friendship.getLower().getOwnerId(), friendship.getHigher().getOwnerId()))
                .toList());
        if (!pairs.isEmpty()) {
            jdbc.update("""
                    DELETE FROM friendships WHERE (low_user_id, high_user_id) IN (:pairs)
                    ORDER BY low_user_id, high_user_id
                    """, Map.of("pairs", pairs));
        }
    }

    private static List<Object[]> pairParameters(Collection<UserPairKey> keys) {
        return keys.stream()
                .filter(pair -> pair.low() < pair.high())
                .distinct()
                .map(pair -> new Object[]{pair.low(), pair.high()})
                .toList();
    }

    private static void bindRecognition(MapSqlParameterSource parameters, String side, FriendRecognition recognition) {
        parameters.addValue(side + "Alias", storedAlias(recognition.getFriendAlias()), Types.VARCHAR)
                .addValue(side + "Score", recognition.getInterestScore())
                .addValue(side + "InteractedAt", recognition.getLastInteractedAt(), Types.TIMESTAMP)
                .addValue(side + "Muted", recognition.isMuted())
                .addValue(side + "Routable", recognition.isRoutable());
    }

    private static String storedAlias(String alias) {
        return alias.isEmpty() ? null : alias;
    }

    private List<Friendship> restore(List<FriendshipRow> rows) {
        Set<Long> userIds = rows.stream().flatMap(row -> Stream.of(row.key().low(), row.key().high()))
                .collect(Collectors.toSet());
        Map<Long, SocialUserProfile> profiles = users.findActiveProfiles(userIds);
        return rows.stream()
                .filter(row -> profiles.containsKey(row.key().low()) && profiles.containsKey(row.key().high()))
                .map(FriendshipRow::toFriendship)
                .toList();
    }

    private Set<Long> activeFriendIds(Long userId, List<Long> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<Long> userIds = new HashSet<>(ids);
        userIds.add(userId);
        Map<Long, SocialUserProfile> profiles = users.findActiveProfiles(userIds);
        if (!profiles.containsKey(userId)) {
            return Set.of();
        }
        return ids.stream().filter(profiles::containsKey).collect(Collectors.toSet());
    }

    private record FriendshipRow(UserPairKey key, LocalDate createdAt, double intimacy,
                                 RecognitionRow low, RecognitionRow high) {
        static FriendshipRow read(ResultSet rs, int rowNumber) throws SQLException {
            var key = new UserPairKey(rs.getLong("low_user_id"), rs.getLong("high_user_id"));
            return new FriendshipRow(key, rs.getObject("created_at", LocalDate.class), rs.getDouble("intimacy"),
                    RecognitionRow.read(rs, "low"), RecognitionRow.read(rs, "high"));
        }

        Friendship toFriendship() {
            return Friendship.restore(createdAt, intimacy, low.toRecognition(key.low()), high.toRecognition(key.high()));
        }
    }

    private record RecognitionRow(String alias, double score, LocalDateTime interactedAt,
                                  boolean muted, boolean routable) {
        static RecognitionRow read(ResultSet rs, String side) throws SQLException {
            return new RecognitionRow(rs.getString(side + "_friend_alias"), rs.getDouble(side + "_interest_score"),
                    rs.getObject(side + "_last_interacted_at", LocalDateTime.class),
                    rs.getBoolean(side + "_is_muted"), rs.getBoolean(side + "_is_routable"));
        }

        FriendRecognition toRecognition(long userId) {
            return FriendRecognition.restore(userId, alias, score, interactedAt, muted, routable);
        }
    }
}
