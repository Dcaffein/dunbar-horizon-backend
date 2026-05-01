package com.example.DunbarHorizon.trace.adapter.out.persistence;

import com.example.DunbarHorizon.trace.domain.model.Trace;
import com.example.DunbarHorizon.trace.domain.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TraceRepositoryAdapter implements TraceRepository {
    private final TraceJpaRepository traceJpaRepository;

    @Override
    public Optional<Trace> findByUserAIdAndUserBId(Long id1, Long id2) {
        Trace.TraceIdPair sorted = Trace.sortIds(id1, id2);
        return traceJpaRepository.findByUserAIdAndUserBId(sorted.minId(), sorted.maxId());
    }

    @Override
    public Trace save(Trace trace) {
        return traceJpaRepository.save(trace);
    }
}
