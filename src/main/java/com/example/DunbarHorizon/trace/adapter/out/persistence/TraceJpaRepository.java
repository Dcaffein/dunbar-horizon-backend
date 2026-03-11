package com.example.DunbarHorizon.trace.adapter.out.persistence;

import com.example.DunbarHorizon.trace.domain.model.Trace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraceJpaRepository extends JpaRepository<Trace, Long> {
    Optional<Trace> findByVisitorIdAndTargetId(Long visitorId, Long targetId);
}
