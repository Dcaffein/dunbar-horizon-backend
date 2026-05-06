package com.example.DunbarHorizon.trace.application;

import com.example.DunbarHorizon.trace.application.port.in.TraceCommandUseCase;
import com.example.DunbarHorizon.trace.domain.model.Trace;
import com.example.DunbarHorizon.trace.domain.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TraceService implements TraceCommandUseCase {

    private final TraceRepository traceRepository;

    @Retryable(retryFor = {DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class}, maxAttempts = 3)
    @Transactional
    public void recordTrace(Long visitorId, Long targetId) {
        Trace trace = traceRepository.findByUserAIdAndUserBId(visitorId, targetId)
                .orElseGet(() -> new Trace(visitorId, targetId));

        trace.recordVisit(visitorId);

        traceRepository.save(trace);
    }
}
