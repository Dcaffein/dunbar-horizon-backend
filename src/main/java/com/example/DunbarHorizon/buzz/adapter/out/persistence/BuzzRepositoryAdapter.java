package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzJpaRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        return buzzJpaRepository.findById(buzzId);
    }

    @Override
    public Optional<Buzz> findByIdForShare(Long buzzId) {
        return buzzJpaRepository.findByIdForShare(buzzId);
    }

    @Override
    public void deleteById(Long buzzId) {
        buzzJpaRepository.deleteById(buzzId);
    }

    @Override
    public List<Long> findExpiredIds(LocalDateTime now) {
        return buzzJpaRepository.findIdsByExpiresAtAtOrBefore(now);
    }

    @Override
    public void deleteAllByIds(Collection<Long> buzzIds) {
        buzzJpaRepository.deleteAllByIdIn(buzzIds);
    }
}
