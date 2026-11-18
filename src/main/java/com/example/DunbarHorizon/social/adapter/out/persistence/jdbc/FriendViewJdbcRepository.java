package com.example.DunbarHorizon.social.adapter.out.persistence.jdbc;

import com.example.DunbarHorizon.social.adapter.out.persistence.FriendshipKey;
import com.example.DunbarHorizon.social.application.dto.FriendView;
import com.example.DunbarHorizon.social.application.port.out.FriendViewRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendViewJdbcRepository implements FriendViewRepository {

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

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<FriendView> findFriends(Long userId) {
        return namedParameterJdbcTemplate.query(SELECT_FRIENDS, Map.of("userId", userId), viewOf(userId));
    }

    @Override
    public Optional<FriendView> findFriend(Long userId, Long friendId) {
        var key = FriendshipKey.of(userId, friendId);
        return namedParameterJdbcTemplate.query(SELECT_FRIEND,
                Map.of("userId", userId, "friendId", friendId, "low", key.low(), "high", key.high()),
                viewOf(userId)).stream().findFirst();
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
