package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.social.application.port.out.UserProfilePort;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class FriendshipMysqlRepository {

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
            ON DUPLICATE KEY UPDATE low_user_id = :low
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

    private final NamedParameterJdbcTemplate jdbc;
    private final UserProfilePort userProfiles;

    public void create(Friendship friendship) {
        var recognitions = friendship.getRecognitions().stream()
                .sorted(Comparator.comparing(recognition -> recognition.getUser().getId()))
                .toList();
        FriendRecognition low = recognitions.getFirst();
        FriendRecognition high = recognitions.getLast();
        var parameters = new MapSqlParameterSource()
                .addValue("low", low.getUser().getId())
                .addValue("high", high.getUser().getId())
                .addValue("createdAt", friendship.getCreatedAt(), Types.DATE)
                .addValue("intimacy", friendship.getIntimacy());
        bindRecognition(parameters, "low", low);
        bindRecognition(parameters, "high", high);
        jdbc.update(INSERT, parameters);
    }

    public Optional<Friendship> findById(String id) {
        return findPair(id, SELECT_PAIR);
    }

    // The caller keeps the lock through the settings update in the same transaction.
    public Optional<Friendship> findByIdForUpdate(String id) {
        return findPair(id, SELECT_PAIR + " FOR UPDATE");
    }

    private Optional<Friendship> findPair(String id, String sql) {
        return PairKey.parse(id)
                .flatMap(pair -> restore(jdbc.query(sql, pair.parameters(), FriendshipRow::read)).stream().findFirst());
    }

    public boolean existsFriendshipBetween(Long userId, Long targetId) {
        var key = new PairKey(Math.min(userId, targetId), Math.max(userId, targetId));
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM friendships WHERE " + PAIR + ")", key.parameters(), Boolean.class));
    }

    public List<Friendship> findByUserId(Long userId) {
        return restore(jdbc.query(SELECT_FRIENDSHIPS, Map.of("userId", userId), FriendshipRow::read));
    }

    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted) {
        List<Long> ids = jdbc.queryForList(SELECT_BY_MUTE_STATUS, Map.of("userId", userId, "muted", isMuted), Long.class);
        return activeFriendIds(userId, ids);
    }

    public Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = jdbc.queryForList(SELECT_FRIEND_IDS_AMONG,
                Map.of("userId", userId, "candidateIds", candidateIds), Long.class);
        return activeFriendIds(userId, ids);
    }

    public void updateUserFields(Friendship friendship, Long userId) {
        String side = userId < friendship.getFriend(userId).getId() ? "low" : "high";
        PairKey pair = PairKey.parse(friendship.getId()).orElseThrow();
        var parameters = new MapSqlParameterSource(pair.parameters())
                .addValue("alias", friendship.getFriendAlias(userId), Types.VARCHAR)
                .addValue("muted", friendship.isMuted(userId))
                .addValue("routable", friendship.isRoutable(userId));
        jdbc.update("""
                UPDATE friendships
                SET %1$s_friend_alias = :alias, %1$s_is_muted = :muted, %1$s_is_routable = :routable
                WHERE %2$s
                """.formatted(side, PAIR), parameters);
    }

    public void delete(String friendshipId) {
        PairKey.parse(friendshipId)
                .ifPresent(pair -> jdbc.update("DELETE FROM friendships WHERE " + PAIR, pair.parameters()));
    }

    private static void bindRecognition(MapSqlParameterSource parameters, String side, FriendRecognition recognition) {
        parameters.addValue(side + "Alias", recognition.getFriendAlias(), Types.VARCHAR)
                .addValue(side + "Score", recognition.getInterestScore())
                .addValue(side + "InteractedAt", recognition.getLastInteractedAt(), Types.TIMESTAMP)
                .addValue(side + "Muted", recognition.isMuted())
                .addValue(side + "Routable", recognition.isRoutable());
    }

    private List<Friendship> restore(List<FriendshipRow> rows) {
        Set<Long> userIds = rows.stream().flatMap(row -> Stream.of(row.key().low(), row.key().high()))
                .collect(Collectors.toSet());
        Map<Long, UserProfileInfo> profiles = activeProfiles(userIds);
        return rows.stream()
                .filter(row -> profiles.containsKey(row.key().low()) && profiles.containsKey(row.key().high()))
                .map(row -> row.toFriendship(profiles.get(row.key().low()), profiles.get(row.key().high())))
                .toList();
    }

    private Set<Long> activeFriendIds(Long userId, List<Long> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<Long> userIds = new HashSet<>(ids);
        userIds.add(userId);
        Map<Long, UserProfileInfo> profiles = activeProfiles(userIds);
        if (!profiles.containsKey(userId)) {
            return Set.of();
        }
        return ids.stream().filter(profiles::containsKey).collect(Collectors.toSet());
    }

    private Map<Long, UserProfileInfo> activeProfiles(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userProfiles.getUserProfilesForSync(ids).stream()
                .collect(Collectors.toMap(UserProfileInfo::id, Function.identity()));
    }

    private record PairKey(long low, long high) {
        static Optional<PairKey> parse(String id) {
            String[] ids = id.split("_", -1);
            if (ids.length != 2) {
                return Optional.empty();
            }
            try {
                var pair = new PairKey(Long.parseLong(ids[0]), Long.parseLong(ids[1]));
                return pair.low < pair.high && pair.id().equals(id) ? Optional.of(pair) : Optional.empty();
            } catch (NumberFormatException invalidId) {
                return Optional.empty();
            }
        }

        String id() {
            return Friendship.generateCompositeId(low, high);
        }

        Map<String, Long> parameters() {
            return Map.of("low", low, "high", high);
        }
    }

    private record FriendshipRow(PairKey key, LocalDate createdAt, double intimacy,
                                 RecognitionRow low, RecognitionRow high) {
        static FriendshipRow read(ResultSet rs, int rowNumber) throws SQLException {
            var key = new PairKey(rs.getLong("low_user_id"), rs.getLong("high_user_id"));
            return new FriendshipRow(key, rs.getObject("created_at", LocalDate.class), rs.getDouble("intimacy"),
                    RecognitionRow.read(rs, "low"), RecognitionRow.read(rs, "high"));
        }

        Friendship toFriendship(UserProfileInfo lowProfile, UserProfileInfo highProfile) {
            return Friendship.restore(key.id(), createdAt, intimacy,
                    List.of(low.toRecognition(lowProfile), high.toRecognition(highProfile)));
        }
    }

    private record RecognitionRow(String alias, double score, LocalDateTime interactedAt,
                                  boolean muted, boolean routable) {
        static RecognitionRow read(ResultSet rs, String side) throws SQLException {
            return new RecognitionRow(rs.getString(side + "_friend_alias"), rs.getDouble(side + "_interest_score"),
                    rs.getObject(side + "_last_interacted_at", LocalDateTime.class),
                    rs.getBoolean(side + "_is_muted"), rs.getBoolean(side + "_is_routable"));
        }

        FriendRecognition toRecognition(UserProfileInfo profile) {
            return FriendRecognition.restore(new AccountReference(profile), alias, score, interactedAt, muted, routable);
        }
    }

    private record AccountReference(UserProfileInfo profile) implements UserReference {
        @Override
        public Long getId() {
            return profile.id();
        }

        @Override
        public String getNickname() {
            return profile.nickname();
        }

        @Override
        public String getProfileImageUrl() {
            return profile.profileImage();
        }
    }
}
