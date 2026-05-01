package com.example.DunbarHorizon.trace.domain.repository;

import com.example.DunbarHorizon.trace.domain.model.Trace;

import java.util.Optional;

public interface TraceRepository {
    Optional<Trace> findByUserAIdAndUserBId(Long id1, Long id2);
    Trace save(Trace trace);
}