package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.Buzz;

import java.util.Optional;

public interface BuzzRepository {
    Buzz save(Buzz buzz);
    Optional<Buzz> findById(String buzzId);
    Optional<Buzz> findByIdForShare(String buzzId);
    void deleteById(String buzzId);
}
