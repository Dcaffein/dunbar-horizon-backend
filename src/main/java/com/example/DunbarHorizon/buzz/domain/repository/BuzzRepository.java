package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.Buzz;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuzzRepository {
    Buzz save(Buzz buzz);
    Optional<Buzz> findById(Long buzzId);
    Optional<Buzz> findByIdForShare(Long buzzId);
    void deleteById(Long buzzId);
    List<Long> findExpiredIds(LocalDateTime now);
    void deleteAllByIds(Collection<Long> buzzIds);
}
