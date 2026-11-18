package com.example.DunbarHorizon.social.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;

final class LabelMembershipTestQueries {

    private LabelMembershipTestQueries() {
    }

    static boolean hasLabelMember(JdbcTemplate jdbc, String labelId, Long memberId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM label_members WHERE label_id = ? AND member_id = ?)
                """, Boolean.class, labelId, memberId));
    }
}
