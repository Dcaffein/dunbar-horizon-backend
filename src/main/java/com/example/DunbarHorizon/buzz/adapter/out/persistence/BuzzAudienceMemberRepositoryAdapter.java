package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzAudienceMemberJpaRepository;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzAudienceMemberRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuzzAudienceMemberRepositoryAdapter implements BuzzAudienceMemberRepository {

    private static final String DELETE_BY_BUZZ_IDS = "DELETE FROM buzz_audience_members WHERE buzz_id IN (:buzzIds)";

    private final BuzzAudienceMemberJpaRepository buzzAudienceMemberJpaRepository;
    private final EntityManager entityManager;
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public BuzzAudienceMember save(BuzzAudienceMember buzzAudienceMember) {
        return buzzAudienceMemberJpaRepository.save(buzzAudienceMember);
    }

    @Override
    public List<BuzzAudienceMember> saveAll(List<BuzzAudienceMember> buzzAudienceMembers) {
        buzzAudienceMembers.forEach(entityManager::persist);
        return buzzAudienceMembers;
    }

    @Override
    public Optional<BuzzAudienceMember> findByBuzzIdAndUserId(Long buzzId, Long userId) {
        return buzzAudienceMemberJpaRepository.findByBuzzIdAndUserId(buzzId, userId);
    }

    @Override
    public void deleteAllByBuzzIds(Collection<Long> buzzIds) {
        if (buzzIds.isEmpty()) {
            return;
        }
        jdbc.update(DELETE_BY_BUZZ_IDS, Map.of("buzzIds", buzzIds));
    }
}
