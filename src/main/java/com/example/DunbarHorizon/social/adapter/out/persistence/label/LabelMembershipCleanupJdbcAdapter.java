package com.example.DunbarHorizon.social.adapter.out.persistence.label;

import com.example.DunbarHorizon.social.application.label.port.LabelMembershipCleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class LabelMembershipCleanupJdbcAdapter implements LabelMembershipCleanup {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void removeMembershipsBetween(Long firstUserId, Long secondUserId) {
        List<Object[]> directions = List.of(
                new Object[]{firstUserId, secondUserId}, new Object[]{secondUserId, firstUserId});

        jdbc.update("""
                DELETE lm FROM label_members lm
                JOIN labels l ON l.id = lm.label_id
                WHERE (l.owner_id, lm.member_id) IN (:directions)
                """, Map.of("directions", directions));
    }
}
