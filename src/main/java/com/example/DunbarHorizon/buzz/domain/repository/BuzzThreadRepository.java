package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;

import java.util.List;
import java.util.Optional;

public interface BuzzThreadRepository {
    BuzzThread save(BuzzThread buzzThread);
    Optional<BuzzThread> findById(Long threadId);
    List<BuzzThread> findAllByBuzzId(String buzzId);
    void deleteAllByBuzzId(String buzzId);
}
