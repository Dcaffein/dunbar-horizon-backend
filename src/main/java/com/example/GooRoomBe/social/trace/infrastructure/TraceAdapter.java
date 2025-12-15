package com.example.GooRoomBe.social.trace.infrastructure;

import com.example.GooRoomBe.social.trace.domain.Trace;
import com.example.GooRoomBe.social.trace.domain.TracePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TraceAdapter implements TracePort {

    private final TraceRepository traceRepository;

    @Override
    public Optional<Trace> findTrace(String visitorId, String targetId) {
        return traceRepository.findByVisitorIdAndTargetId(visitorId, targetId);
    }

    @Override
    public int getVisitCount(String visitorId, String targetId) {
        return traceRepository.getVisitCount(visitorId, targetId);
    }

    @Override
    public void save(Trace trace, String visitorId) {
        traceRepository.saveTrace(
                visitorId,
                trace.getTarget().getId(),
                trace.getCount(),
                trace.getLastVisitedAt()
        );
    }
}