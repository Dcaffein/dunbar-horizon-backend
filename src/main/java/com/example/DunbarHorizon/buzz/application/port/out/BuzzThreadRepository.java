package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;

import java.util.Optional;

public interface BuzzThreadRepository {
    BuzzThread save(BuzzThread buzzThread);
    Optional<BuzzThread> findById(Long threadId);
    Optional<BuzzThread> findByIdForShare(Long threadId);
    void deleteById(Long threadId);
}
