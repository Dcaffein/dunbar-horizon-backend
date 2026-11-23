package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzAudienceMemberJpaRepository;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzAudienceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuzzAudienceMemberRepositoryAdapter implements BuzzAudienceMemberRepository {

    private static final String INSERT = """
            INSERT INTO buzz_audience_members (buzz_id, user_id, is_read)
            VALUES (:buzzId, :userId, :isRead)
            """;

    private final BuzzAudienceMemberJpaRepository buzzAudienceMemberJpaRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public BuzzAudienceMember save(BuzzAudienceMember buzzAudienceMember) {
        return buzzAudienceMemberJpaRepository.save(buzzAudienceMember);
    }

    @Override
    public List<BuzzAudienceMember> saveAll(List<BuzzAudienceMember> buzzAudienceMembers) {
        jdbc.batchUpdate(INSERT, buzzAudienceMembers.stream()
                .map(member -> new MapSqlParameterSource()
                        .addValue("buzzId", member.getBuzzId())
                        .addValue("userId", member.getUserId())
                        .addValue("isRead", member.isRead()))
                .toArray(SqlParameterSource[]::new));
        return buzzAudienceMembers;
    }

    @Override
    public Optional<BuzzAudienceMember> findByBuzzIdAndUserId(Long buzzId, Long userId) {
        return buzzAudienceMemberJpaRepository.findByBuzzIdAndUserId(buzzId, userId);
    }
}
