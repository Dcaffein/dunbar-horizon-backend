package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class FriendshipRows {

    private FriendshipRows() {
    }

    static Friendship read(ResultSet rs, int rowNumber) throws SQLException {
        return Friendship.restore(rs.getObject("created_at", LocalDate.class),
                readRecognition(rs, "low"), readRecognition(rs, "high"));
    }

    static MapSqlParameterSource insertParameters(Friendship friendship) {
        var parameters = new MapSqlParameterSource()
                .addValue("createdAt", friendship.getCreatedAt(), Types.DATE)
                .addValue("intimacy", friendship.getIntimacy());
        bindRecognition(parameters, "low", friendship.getLower());
        bindRecognition(parameters, "high", friendship.getHigher());
        return parameters;
    }

    static String storedAlias(String alias) {
        return alias.isEmpty() ? null : alias;
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

    private static void bindRecognition(MapSqlParameterSource parameters, String side, FriendRecognition recognition) {
        parameters.addValue(side, recognition.getOwnerId())
                .addValue(side + "Alias", storedAlias(recognition.getFriendAlias()), Types.VARCHAR)
                .addValue(side + "Score", recognition.getInterestScore())
                .addValue(side + "InteractedAt", recognition.getLastInteractedAt(), Types.TIMESTAMP)
                .addValue(side + "Muted", recognition.isMuted())
                .addValue(side + "Routable", recognition.isRoutable());
    }
}
