package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzJpaRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
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

    @Override
    public Buzz save(Buzz buzz) {
        return buzzJpaRepository.save(buzz);
    }

    @Override
    public Optional<Buzz> findById(String buzzId) {
        return buzzJpaRepository.findById(buzzId);
    }

    @Override
    public Optional<Buzz> findByIdForShare(String buzzId) {
        return buzzJpaRepository.findByIdForShare(buzzId);
    }

    @Override
    public void deleteById(String buzzId) {
        buzzJpaRepository.deleteById(buzzId);
    }

    @Override
    public List<String> findExpiredIds(LocalDateTime now) {
        return buzzJpaRepository.findIdsByExpiresAtAtOrBefore(now);
    }

    @Override
    public void deleteAllByIds(Collection<String> buzzIds) {
        buzzJpaRepository.deleteAllByIdIn(buzzIds);
    }
}
