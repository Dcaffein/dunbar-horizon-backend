package com.example.DunbarHorizon.social.adapter.out.persistence.friendrequest;

import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendRequestView;
import com.example.DunbarHorizon.social.application.friendrequest.port.FriendRequestQueryRepository;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendRequestQueryJdbcRepository implements FriendRequestQueryRepository {

    private static final String SELECT_REQUEST = """
            SELECT r.requester_id, r.receiver_id, r.status, r.created_at,
                   requester.nickname AS requester_nickname, receiver.nickname AS receiver_nickname
            FROM friend_requests r
            """;

    private static final String SELECT_WITH_ACTIVE_PAIR = SELECT_REQUEST + """
            JOIN users requester ON requester.user_id = r.requester_id AND requester.status = 'ACTIVE'
            JOIN users receiver ON receiver.user_id = r.receiver_id AND receiver.status = 'ACTIVE'
            """;

    private static final String SELECT_SENT = SELECT_WITH_ACTIVE_PAIR + "WHERE r.requester_id = :requesterId";

    private static final String SELECT_SENT_TO = SELECT_REQUEST + """
            LEFT JOIN users requester ON requester.user_id = r.requester_id AND requester.status = 'ACTIVE'
            LEFT JOIN users receiver ON receiver.user_id = r.receiver_id AND receiver.status = 'ACTIVE'
            WHERE r.requester_id = :requesterId AND r.receiver_id = :receiverId
            """;

    private static final String SELECT_RECEIVED = SELECT_WITH_ACTIVE_PAIR
            + "WHERE r.receiver_id = :receiverId AND r.status = :status";

    private static final RowMapper<FriendRequestView> VIEW = (rs, rowNumber) -> new FriendRequestView(
            rs.getLong("requester_id"),
            rs.getString("requester_nickname"),
            rs.getLong("receiver_id"),
            rs.getString("receiver_nickname"),
            FriendRequestStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", LocalDateTime.class));

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public List<FriendRequestView> findSent(Long requesterId) {
        return namedParameterJdbcTemplate.query(SELECT_SENT, Map.of("requesterId", requesterId), VIEW);
    }

    @Override
    public Optional<FriendRequestView> findSentTo(Long requesterId, Long receiverId) {
        return namedParameterJdbcTemplate.query(SELECT_SENT_TO,
                Map.of("requesterId", requesterId, "receiverId", receiverId), VIEW).stream().findFirst();
    }

    @Override
    public List<FriendRequestView> findReceived(Long receiverId, FriendRequestStatus status) {
        return namedParameterJdbcTemplate.query(SELECT_RECEIVED,
                Map.of("receiverId", receiverId, "status", status.name()), VIEW);
    }
}
