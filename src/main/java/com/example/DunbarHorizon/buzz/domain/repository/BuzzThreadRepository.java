package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuzzThreadRepository {
    BuzzThread save(BuzzThread buzzThread);
    Optional<BuzzThread> findById(Long threadId);
    Optional<BuzzThread> findByIdForShare(Long threadId);
    List<Long> findIdsByBuzzIds(Collection<Long> buzzIds);
    void deleteById(Long threadId);
    void deleteAllByBuzzIds(Collection<Long> buzzIds);
}
