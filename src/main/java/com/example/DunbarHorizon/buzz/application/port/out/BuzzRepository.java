package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.domain.Buzz;

import java.util.Optional;

public interface BuzzRepository {
    Buzz save(Buzz buzz);
    Optional<Buzz> findById(Long buzzId);
    Optional<Buzz> findByIdForShare(Long buzzId);
}
