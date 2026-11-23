package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzJpaRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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


}
