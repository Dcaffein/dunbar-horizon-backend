package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzJpaRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
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

    @Override
    public Buzz save(Buzz buzz) {
        return buzzJpaRepository.save(buzz);
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
    public List<Long> findPurgeableIds(LocalDateTime createdAtOrBefore) {
        Set<Long> buzzIds = new LinkedHashSet<>(buzzJpaRepository.findIdsByCreatedAtAtOrBefore(createdAtOrBefore));
        buzzIds.addAll(buzzJpaRepository.findIdsByDeletedAtIsNotNull());
        return List.copyOf(buzzIds);
    }

    @Override
    public void deleteAllByIds(Collection<Long> buzzIds) {
        buzzJpaRepository.deleteAllByIdIn(buzzIds);
    }
}
