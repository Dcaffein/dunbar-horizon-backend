package com.example.GooRoomBe.trace.domain.repository;

import com.example.GooRoomBe.trace.domain.model.Trace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraceRepository {
    Optional<Trace> findByVisitorAndTarget(Long visitorId, Long targetId);
    Trace save(Trace trace);
}