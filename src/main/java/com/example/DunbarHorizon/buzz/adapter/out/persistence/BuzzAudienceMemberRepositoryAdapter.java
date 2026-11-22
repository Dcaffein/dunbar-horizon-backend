package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzAudienceMemberJpaRepository;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuzzAudienceMemberRepositoryAdapter implements BuzzAudienceMemberRepository {

    private final BuzzAudienceMemberJpaRepository buzzAudienceMemberJpaRepository;
    private final EntityManager entityManager;

    @Override
    public List<BuzzAudienceMember> saveAll(List<BuzzAudienceMember> buzzAudienceMembers) {
        buzzAudienceMembers.forEach(entityManager::persist);
        return buzzAudienceMembers;
    }

    @Override
    public Optional<BuzzAudienceMember> findByBuzzIdAndUserId(String buzzId, Long userId) {
        return buzzAudienceMemberJpaRepository.findByBuzzIdAndUserId(buzzId, userId);
    }

    @Override
    public List<Long> findUserIdsByBuzzId(String buzzId) {
        return buzzAudienceMemberJpaRepository.findUserIdsByBuzzId(buzzId);
    }

    @Override
    public void deleteAllByBuzzId(String buzzId) {
        buzzAudienceMemberJpaRepository.deleteAllByBuzzId(buzzId);
    }

    @Override
    public void deleteAllByBuzzIds(Collection<String> buzzIds) {
        buzzAudienceMemberJpaRepository.deleteAllByBuzzIdIn(buzzIds);
    }
}
