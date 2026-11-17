package com.example.DunbarHorizon.social.adapter.out.persistence.jdbc;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;
import com.example.DunbarHorizon.social.application.port.out.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserProfileJdbcRepository implements UserProfileRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Map<Long, SocialUserProfile> findActiveProfiles(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jdbc.query("""
                SELECT user_id, nickname, profile_image
                FROM users
                WHERE user_id IN (:ids) AND status = 'ACTIVE'
                """, Map.of("ids", ids), (row, index) -> new SocialUserProfile(
                        row.getLong("user_id"), row.getString("nickname"), row.getString("profile_image")))
                .stream()
                .collect(Collectors.toMap(SocialUserProfile::id, Function.identity()));
    }
}
