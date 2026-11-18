package com.example.DunbarHorizon.social.adapter.out.persistence.jdbc;

import com.example.DunbarHorizon.social.application.dto.result.LabelMemberResult;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.application.port.out.LabelMembershipRepository;
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
public class LabelJdbcRepository implements LabelQueryRepository, LabelMembershipRepository {

    private static final String SELECT_LABELS = """
            SELECT l.id, l.name, COUNT(u.user_id) AS member_count
            FROM labels l
            JOIN users o ON o.user_id = l.owner_id AND o.status = 'ACTIVE'
            LEFT JOIN label_members lm ON lm.label_id = l.id
            LEFT JOIN users u ON u.user_id = lm.member_id AND u.status = 'ACTIVE'
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public boolean existsMember(String labelId, Long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM label_members WHERE label_id = :labelId AND member_id = :memberId)
                """, Map.of("labelId", labelId, "memberId", memberId), Boolean.class));
    }

    @Override
    public void removeMembersBetween(Long userId, Long friendId) {
        List<Object[]> directions = List.of(new Object[]{userId, friendId}, new Object[]{friendId, userId});

        // The caller deleted this friendship first, so its memberships cannot be added concurrently.
        List<String> affectedLabels = jdbc.queryForList("""
                SELECT DISTINCT l.id FROM labels l
                JOIN label_members lm ON lm.label_id = l.id
                WHERE (l.owner_id, lm.member_id) IN (:directions)
                """, Map.of("directions", directions), String.class);
        if (affectedLabels.isEmpty()) {
            return;
        }

        jdbc.queryForList("""
                SELECT id FROM labels FORCE INDEX (PRIMARY)
                WHERE id IN (:ids) ORDER BY id FOR UPDATE
                """, Map.of("ids", affectedLabels), String.class);
        jdbc.update("""
                DELETE lm FROM label_members lm
                JOIN labels l ON l.id = lm.label_id
                WHERE l.id IN (:ids) AND (l.owner_id, lm.member_id) IN (:directions)
                """, Map.of("ids", affectedLabels, "directions", directions));
    }

    @Override
    public List<LabelResult> findAllByOwner(Long ownerId) {
        return findResults("WHERE l.owner_id = :ownerId", Map.of("ownerId", ownerId));
    }

    @Override
    public Optional<LabelResult> findResultById(String labelId) {
        return findResults("WHERE l.id = :id", Map.of("id", labelId)).stream().findFirst();
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
    public Set<Long> findMemberIdsByOwnerAndLabelIds(Long ownerId, List<String> labelIds) {
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
