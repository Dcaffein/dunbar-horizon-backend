package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;
import com.example.DunbarHorizon.social.application.port.out.FriendRequestCommandRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class FriendRequestMysqlRepository implements FriendRequestCommandRepository {

    private static final String PAIR = "low_user_id = :low AND high_user_id = :high";
    private static final String SELECT = "SELECT requester_id, receiver_id, created_at, status FROM friend_requests";
    private static final String SELECT_PAIR = SELECT + " WHERE " + PAIR;

    private final NamedParameterJdbcTemplate jdbc;
    private final SocialUserMysqlRepository users;

    @Override
    public FriendRequest saveRequest(FriendRequest request) {
        long requesterId = request.getRequesterId();
        long receiverId = request.getReceiverId();
        UserPairKey pair = UserPairKey.of(requesterId, receiverId);
        try {
            jdbc.update("""
                    INSERT INTO friend_requests (low_user_id, high_user_id, requester_id, receiver_id, created_at, status)
                    VALUES (:low, :high, :requester, :receiver, :createdAt, :status)
                    """, Map.of("low", pair.low(), "high", pair.high(), "requester", requesterId, "receiver", receiverId,
                    "createdAt", request.getCreatedAt(), "status", request.getStatus().name()));
        } catch (DuplicateKeyException e) {
            throw new DuplicateFriendRequestException(requesterId, receiverId);
        }
        return request;
    }

    @Override
    public FriendRequest updateStatus(FriendRequest request) {
        UserPairKey pair = UserPairKey.of(request.getRequesterId(), request.getReceiverId());
        jdbc.update("UPDATE friend_requests SET status = :status WHERE " + PAIR,
                Map.of("low", pair.low(), "high", pair.high(), "status", request.getStatus().name()));
        return request;
    }

    @Override
    public boolean existsRequestBetween(Long requesterId, Long receiverId) {
        UserPairKey pair = UserPairKey.of(requesterId, receiverId);
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM friend_requests
                    WHERE low_user_id = :low AND high_user_id = :high AND requester_id = :requester)
                """, Map.of("low", pair.low(), "high", pair.high(), "requester", requesterId), Boolean.class));
    }

    @Override
    public Optional<FriendRequest> findById(String requestId) {
        return find(requestId, SELECT_PAIR);
    }

    @Override
    public Optional<FriendRequest> findByIdForUpdate(String requestId) {
        return find(requestId, SELECT_PAIR + " FOR UPDATE");
    }

    private Optional<FriendRequest> find(String id, String sql) {
        return UserPairKey.parse(id)
                .flatMap(pair -> restore(jdbc.query(sql, pair.parameters(), RequestRow::read)).stream().findFirst());
    }

    @Override
    public void deleteById(String requestId) {
        UserPairKey.parse(requestId)
                .ifPresent(pair -> jdbc.update("DELETE FROM friend_requests WHERE " + PAIR, pair.parameters()));
    }

    @Override
    public List<FriendRequest> findAllByReceiver_IdAndStatus(Long receiverId, FriendRequestStatus status) {
        return findAll(SELECT + " WHERE receiver_id = :userId AND status = :status", receiverId, status);
    }

    @Override
    public List<FriendRequest> findAllByRequester_IdAndStatus(Long requesterId, FriendRequestStatus status) {
        return findAll(SELECT + " WHERE requester_id = :userId AND status = :status", requesterId, status);
    }

    private List<FriendRequest> findAll(String sql, Long userId, FriendRequestStatus status) {
        return restore(jdbc.query(sql, Map.of("userId", userId, "status", status.name()), RequestRow::read));
    }

    @Override
    public void deleteOldHiddenRequests(LocalDateTime threshold) {
        jdbc.update("DELETE FROM friend_requests WHERE status = 'HIDDEN' AND created_at <= :threshold",
                Map.of("threshold", threshold));
    }

    private List<FriendRequest> restore(List<RequestRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        var ids = rows.stream().flatMap(row -> Stream.of(row.requesterId(), row.receiverId()))
                .collect(Collectors.toSet());
        Map<Long, SocialUserProfile> profiles = users.findActiveProfiles(ids);
        return rows.stream()
                .filter(row -> profiles.containsKey(row.requesterId()) && profiles.containsKey(row.receiverId()))
                .map(RequestRow::toRequest)
                .toList();
    }

    private record RequestRow(long requesterId, long receiverId, LocalDateTime createdAt, FriendRequestStatus status) {
        static RequestRow read(ResultSet rs, int rowNumber) throws SQLException {
            return new RequestRow(rs.getLong("requester_id"), rs.getLong("receiver_id"),
                    rs.getObject("created_at", LocalDateTime.class), FriendRequestStatus.valueOf(rs.getString("status")));
        }

        FriendRequest toRequest() {
            return new FriendRequest(FriendRequest.generateId(requesterId, receiverId), requesterId, receiverId, createdAt, status);
        }
    }
}
