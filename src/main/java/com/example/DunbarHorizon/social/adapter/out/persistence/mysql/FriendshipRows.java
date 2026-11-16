package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class FriendshipRows {

    private FriendshipRows() {
    }

    static Friendship read(ResultSet rs, int rowNumber) throws SQLException {
        return Friendship.restore(rs.getObject("created_at", LocalDate.class),
                readRecognition(rs, "low"), readRecognition(rs, "high"));
    }

    private static FriendRecognition readRecognition(ResultSet rs, String side) throws SQLException {
        return FriendRecognition.restore(
                rs.getLong(side + "_user_id"),
                rs.getString(side + "_friend_alias"),
                rs.getDouble(side + "_interest_score"),
                rs.getObject(side + "_last_interacted_at", LocalDateTime.class),
                rs.getBoolean(side + "_is_muted"),
                rs.getBoolean(side + "_is_routable"));
    }
}
