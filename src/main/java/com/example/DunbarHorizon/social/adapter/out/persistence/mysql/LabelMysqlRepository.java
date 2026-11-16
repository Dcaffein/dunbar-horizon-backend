package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.application.port.out.LabelMembershipRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class LabelMysqlRepository implements LabelMembershipRepository {

    private static final String SELECT_LABELS = """
            SELECT l.id, l.name, l.owner_id, u.user_id AS member_id
            FROM labels l
            JOIN users o ON o.user_id = l.owner_id
            LEFT JOIN label_members lm ON lm.label_id = l.id
            LEFT JOIN users u ON u.user_id = lm.member_id AND u.status = 'ACTIVE'
            """;
    private static final String INSERT_MEMBER = """
            INSERT INTO label_members (label_id, member_id) VALUES (:labelId, :memberId)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void create(Label label) {
        Long ownerId = label.getOwnerId();
        try {
            jdbc.update("INSERT INTO labels (id, owner_id, name) VALUES (:id, :owner, :name)",
                    Map.of("id", label.getId(), "owner", ownerId, "name", label.getLabelName()));
        } catch (DuplicateKeyException duplicate) {
            throw new DuplicateLabelNameException(ownerId, label.getLabelName());
        }
    }

    @Override
    public void rename(String labelId, String labelName) {
        try {
            jdbc.update("UPDATE labels SET name = :name WHERE id = :id", Map.of("id", labelId, "name", labelName));
        } catch (DuplicateKeyException duplicate) {
            throw new DuplicateLabelNameException(findOwnerId(labelId).orElseThrow(), labelName);
        }
    }

    @Override
    public void delete(String labelId) {
        jdbc.update("DELETE FROM labels WHERE id = :id", Map.of("id", labelId));
    }

    @Override
    public boolean existsMember(String labelId, Long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM label_members WHERE label_id = :labelId AND member_id = :memberId)
                """, Map.of("labelId", labelId, "memberId", memberId), Boolean.class));
    }

    @Override
    public void addMember(String labelId, Long memberId) {
        try {
            jdbc.update(INSERT_MEMBER, Map.of("labelId", labelId, "memberId", memberId));
        } catch (DuplicateKeyException duplicate) {
            throw new DuplicateLabelMemberException(memberId);
        }
    }

    @Override
    public void removeMember(String labelId, Long memberId) {
        jdbc.update("DELETE FROM label_members WHERE label_id = :labelId AND member_id = :memberId",
                Map.of("labelId", labelId, "memberId", memberId));
    }

    @Override
    public void replaceMembers(String labelId, Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            jdbc.update("DELETE FROM label_members WHERE label_id = :labelId", Map.of("labelId", labelId));
            return;
        }
        jdbc.update("DELETE FROM label_members WHERE label_id = :labelId AND member_id NOT IN (:memberIds)",
                Map.of("labelId", labelId, "memberIds", memberIds));
        var rows = memberIds.stream().distinct().sorted()
                .map(memberId -> Map.of("labelId", labelId, "memberId", memberId))
                .toArray();
        jdbc.batchUpdate(INSERT_MEMBER + " ON DUPLICATE KEY UPDATE member_id = :memberId",
                SqlParameterSourceUtils.createBatch(rows));
    }

    @Override
    public void removeMemberFromOwnedLabels(Long ownerId, Long memberId) {
        jdbc.update("""
                DELETE lm FROM label_members lm
                JOIN labels l ON l.id = lm.label_id
                WHERE l.owner_id = :ownerId AND lm.member_id = :memberId
                """, Map.of("ownerId", ownerId, "memberId", memberId));
    }

    @Override
    public void removeMembersForEndedFriendships(Collection<Friendship> friendships) {
        List<Object[]> directions = friendships.stream()
                .flatMap(friendship -> {
                    Long low = friendship.getLower().getOwnerId();
                    Long high = friendship.getHigher().getOwnerId();
                    return Stream.of(new Object[]{low, high}, new Object[]{high, low});
                })
                .toList();
        if (directions.isEmpty()) {
            return;
        }

        // The caller holds these friendships, so their affected memberships cannot be added concurrently.
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
    public boolean existsByOwner_IdAndLabelName(Long ownerId, String labelName) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM labels WHERE owner_id = :ownerId AND name = :name)
                """, Map.of("ownerId", ownerId, "name", labelName), Boolean.class));
    }

    @Override
    public List<Label> findAllByOwner_Id(Long ownerId) {
        return findLabels("WHERE l.owner_id = :ownerId", Map.of("ownerId", ownerId));
    }

    @Override
    public Optional<Label> findById(String labelId) {
        return findLabels("WHERE l.id = :id", Map.of("id", labelId)).stream().findFirst();
    }

    @Override
    public Optional<Long> findOwnerId(String labelId) {
        return jdbc.queryForList("SELECT owner_id FROM labels WHERE id = :id",
                Map.of("id", labelId), Long.class).stream().findFirst();
    }

    @Override
    public Optional<Long> lockOwnerId(String labelId) {
        return jdbc.queryForList("SELECT owner_id FROM labels WHERE id = :id FOR UPDATE",
                Map.of("id", labelId), Long.class).stream().findFirst();
    }

    @Override
    public List<Label> findLabelsByOwnerAndMember(Long ownerId, Long memberId) {
        return findLabels("""
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

    private List<Label> findLabels(String filter, Map<String, ?> parameters) {
        return jdbc.query(SELECT_LABELS + filter, parameters, rows -> {
            Map<String, LabelRow> labels = new LinkedHashMap<>();
            while (rows.next()) {
                String id = rows.getString("id");
                LabelRow label = labels.get(id);
                if (label == null) {
                    label = new LabelRow(id, rows.getString("name"), rows.getLong("owner_id"), new HashSet<>());
                    labels.put(id, label);
                }
                Long memberId = rows.getObject("member_id", Long.class);
                if (memberId != null) {
                    label.memberIds().add(memberId);
                }
            }
            return labels.values().stream().map(LabelRow::toLabel).toList();
        });
    }

    private record LabelRow(String id, String name, Long ownerId, Set<Long> memberIds) {
        Label toLabel() {
            return Label.restore(id, name, ownerId, memberIds);
        }
    }
}
