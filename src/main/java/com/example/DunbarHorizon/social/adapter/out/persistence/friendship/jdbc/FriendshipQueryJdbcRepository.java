package com.example.DunbarHorizon.social.adapter.out.persistence.friendship.jdbc;

import com.example.DunbarHorizon.social.application.friendship.dto.FriendView;
import com.example.DunbarHorizon.social.application.friendship.dto.UserProfile;
import com.example.DunbarHorizon.social.application.friendship.port.FriendshipQueryRepository;
import com.example.DunbarHorizon.social.domain.friendship.Friendship;
import com.example.DunbarHorizon.social.domain.friendship.FriendshipId;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.DunbarHorizon.social.adapter.out.persistence.friendship.jdbc.FriendshipConditions.ACTIVE_PAIR;

@Repository
@RequiredArgsConstructor
public class FriendshipQueryJdbcRepository implements FriendshipQueryRepository {

    private static final String VIEWER_ACTIVE =
            "EXISTS (SELECT 1 FROM users me WHERE me.user_id = :userId AND me.status = 'ACTIVE')";

    private static final String SELECT_FRIENDS = """
            SELECT f.*, u.nickname AS friend_nickname, u.profile_image AS friend_profile_image
            FROM friendships f
            JOIN users u ON u.user_id = f.high_user_id AND u.status = 'ACTIVE'
            WHERE f.low_user_id = :userId AND %1$s
            UNION ALL
            SELECT f.*, u.nickname AS friend_nickname, u.profile_image AS friend_profile_image
            FROM friendships f
            JOIN users u ON u.user_id = f.low_user_id AND u.status = 'ACTIVE'
            WHERE f.high_user_id = :userId AND %1$s
            """.formatted(VIEWER_ACTIVE);

    private static final String SELECT_FRIEND = """
            SELECT f.*, u.nickname AS friend_nickname, u.profile_image AS friend_profile_image
            FROM friendships f
            JOIN users u ON u.user_id = :friendId AND u.status = 'ACTIVE'
            WHERE f.low_user_id = :low AND f.high_user_id = :high AND %s
            """.formatted(VIEWER_ACTIVE);

    private static final String SELECT_FRIEND_IDS_BY_MUTE_STATUS = """
            SELECT high_user_id FROM friendships
            WHERE low_user_id = :userId AND low_is_muted = :muted AND %1$s
            UNION ALL
            SELECT low_user_id FROM friendships
            WHERE high_user_id = :userId AND high_is_muted = :muted AND %1$s
            """.formatted(ACTIVE_PAIR);

    private static final String SELECT_FRIEND_IDS_IN = """
            SELECT high_user_id FROM friendships
            WHERE low_user_id = :userId AND high_user_id IN (:candidateIds) AND %1$s
            UNION ALL
            SELECT low_user_id FROM friendships
            WHERE high_user_id = :userId AND low_user_id IN (:candidateIds) AND %1$s
            """.formatted(ACTIVE_PAIR);

    private static final String SELECT_ACTIVE_PROFILE = """
            SELECT user_id, nickname, profile_image
            FROM users
            WHERE user_id = :userId AND status = 'ACTIVE'
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<FriendView> findFriends(Long userId) {
        return jdbc.query(SELECT_FRIENDS, Map.of("userId", userId), viewOf(userId));
    }

    @Override
    public Optional<FriendView> findFriend(Long userId, Long friendId) {
        var id = FriendshipId.of(userId, friendId);
        return jdbc.query(SELECT_FRIEND,
                Map.of("userId", userId, "friendId", friendId,
                        "low", id.getLowUserId(), "high", id.getHighUserId()),
                viewOf(userId)).stream().findFirst();
    }

    @Override
    public Set<Long> findFriendIdsIn(Long userId, Collection<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList(SELECT_FRIEND_IDS_IN,
                Map.of("userId", userId, "candidateIds", candidateIds), Long.class));
    }

    @Override
    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean muted) {
        return new HashSet<>(jdbc.queryForList(SELECT_FRIEND_IDS_BY_MUTE_STATUS,
                Map.of("userId", userId, "muted", muted), Long.class));
    }

    @Override
    public Optional<UserProfile> findActiveProfile(Long userId) {
        return jdbc.query(SELECT_ACTIVE_PROFILE, Map.of("userId", userId),
                (row, index) -> new UserProfile(row.getLong("user_id"), row.getString("nickname"),
                        row.getString("profile_image"))).stream().findFirst();
    }

    private static RowMapper<FriendView> viewOf(Long userId) {
        return (rs, rowNumber) -> {
            Friendship friendship = FriendshipRows.read(rs, rowNumber);
            return new FriendView(
                    friendship.getFriendId(userId),
                    rs.getString("friend_nickname"),
                    rs.getString("friend_profile_image"),
                    friendship.getFriendAlias(userId),
                    friendship.getIntimacy(),
                    friendship.getMyNormalizedInterestScore(userId),
                    friendship.isMuted(userId),
                    friendship.isRoutable(userId));
        };
    }
}
