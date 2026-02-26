package com.example.GooRoomBe.trace.adapter.out.persistence;

import com.example.GooRoomBe.trace.domain.model.Trace;
import com.example.GooRoomBe.trace.domain.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TraceRepositoryAdapter implements TraceRepository {
    private final TraceJpaRepository traceJpaRepository;

    @Override
    public Optional<Trace> findByVisitorAndTarget(Long visitorId, Long targetId) {
        return traceJpaRepository.findByVisitorIdAndTargetId(visitorId, targetId);
    }

    @Override
    public Trace save(Trace trace) {
        return traceJpaRepository.save(trace);
    }
}
