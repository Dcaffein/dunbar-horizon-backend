package com.example.DunbarHorizon.social.adapter.out.persistence.jdbc;

import com.example.DunbarHorizon.social.application.dto.result.LabelMemberResult;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.application.port.out.LabelQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class LabelQueryJdbcRepository implements LabelQueryRepository {

    private static final String SELECT_LABELS = """
            SELECT l.id, l.name, COUNT(u.user_id) AS member_count
            FROM labels l
            JOIN users o ON o.user_id = l.owner_id AND o.status = 'ACTIVE'
            LEFT JOIN label_members lm ON lm.label_id = l.id
            LEFT JOIN users u ON u.user_id = lm.member_id AND u.status = 'ACTIVE'
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<LabelResult> findAllByOwner(Long ownerId) {
        return findResults("WHERE l.owner_id = :ownerId", Map.of("ownerId", ownerId));
    }

    @Override
    public Optional<LabelResult> findById(String labelId) {
        return findResults("WHERE l.id = :id", Map.of("id", labelId)).stream().findFirst();
    }

    @Override
    public Optional<Long> findOwnerId(String labelId) {
        return jdbc.queryForList("""
                SELECT l.owner_id
                FROM labels l
                JOIN users o ON o.user_id = l.owner_id AND o.status = 'ACTIVE'
                WHERE l.id = :labelId
                """, Map.of("labelId", labelId), Long.class).stream().findFirst();
    }

    @Override
    public List<LabelMemberResult> findMembers(String labelId) {
        return jdbc.query("""
                SELECT u.user_id, u.nickname
                FROM label_members lm
                JOIN labels l ON l.id = lm.label_id
                JOIN users o ON o.user_id = l.owner_id AND o.status = 'ACTIVE'
                JOIN users u ON u.user_id = lm.member_id AND u.status = 'ACTIVE'
                WHERE lm.label_id = :labelId
                """, Map.of("labelId", labelId), (row, index) ->
                new LabelMemberResult(row.getLong("user_id"), row.getString("nickname")));
    }

    @Override
    public List<LabelResult> findByOwnerAndMember(Long ownerId, Long memberId) {
        return findResults("""
                WHERE l.owner_id = :ownerId AND EXISTS (
                    SELECT 1 FROM label_members selected
                    JOIN users m ON m.user_id = selected.member_id AND m.status = 'ACTIVE'
                    WHERE selected.label_id = l.id AND selected.member_id = :memberId
                )
                """, Map.of("ownerId", ownerId, "memberId", memberId));
    }

    @Override
    public Set<Long> findMemberIdsInOwnedLabels(Long ownerId, List<String> labelIds) {
        if (labelIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(jdbc.queryForList("""
                SELECT DISTINCT lm.member_id
                FROM labels l
                JOIN users o ON o.user_id = l.owner_id AND o.status = 'ACTIVE'
                JOIN label_members lm ON l.id = lm.label_id
                JOIN users m ON m.user_id = lm.member_id AND m.status = 'ACTIVE'
                WHERE l.owner_id = :ownerId AND l.id IN (:labelIds)
                """, Map.of("ownerId", ownerId, "labelIds", labelIds), Long.class));
    }

    private List<LabelResult> findResults(String filter, Map<String, ?> parameters) {
        return jdbc.query(SELECT_LABELS + filter + " GROUP BY l.id, l.name", parameters, (row, index) ->
                new LabelResult(row.getString("id"), row.getString("name"), row.getInt("member_count")));
    }
}
