package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzJpaRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class BuzzRepositoryAdapter implements BuzzRepository {

    private final BuzzJpaRepository buzzJpaRepository;
    private final EntityManager entityManager;

    @Override
    public Buzz save(Buzz buzz) {
        entityManager.persist(buzz);
        return buzz;
    }

    @Override
    public Optional<Buzz> findById(Long buzzId) {
        return buzzJpaRepository.findByIdAndDeletedAtIsNull(buzzId);
    }

    @Override
    public Optional<Buzz> findByIdForShare(Long buzzId) {
        return buzzJpaRepository.findByIdAndDeletedAtIsNullForShare(buzzId);
    }

    @Override
    public Optional<Buzz> findByIdForUpdate(Long buzzId) {
        return buzzJpaRepository.findByIdAndDeletedAtIsNullForUpdate(buzzId);
    }

    @Override
    public List<Long> findPurgeableIds(LocalDateTime now) {
        Set<Long> buzzIds = new LinkedHashSet<>(buzzJpaRepository.findIdsByExpiresAtAtOrBefore(now));
        buzzIds.addAll(buzzJpaRepository.findIdsByDeletedAtIsNotNull());
        return List.copyOf(buzzIds);
    }

    @Override
    public void deleteAllByIds(Collection<Long> buzzIds) {
        buzzJpaRepository.deleteAllByIdIn(buzzIds);
    }
}
