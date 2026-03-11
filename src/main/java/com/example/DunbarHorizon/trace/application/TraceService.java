package com.example.DunbarHorizon.trace.application;

import com.example.DunbarHorizon.trace.adapter.in.web.dto.TraceRecordResponseDto;
import com.example.DunbarHorizon.trace.domain.model.Trace;
import com.example.DunbarHorizon.trace.domain.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceRepository traceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TraceRecordResponseDto recordTrace(Long visitorId, Long targetId) {
        Trace trace = traceRepository.findByVisitorAndTarget(visitorId, targetId)
                .orElseGet(() -> new Trace(visitorId, targetId));

        if (trace.getId() != null) {
            trace.updateVisitCount();
        }

        int partnerCount = traceRepository.findByVisitorAndTarget(targetId, visitorId)
                .map(Trace::getCount)
                .orElse(0);

        traceRepository.save(trace);

        eventPublisher.publishEvent(trace.createInteractionEvent());

        return trace.checkRevealEvent(partnerCount)
                .map(event -> {
                    eventPublisher.publishEvent(event);
                    return TraceRecordResponseDto.revealed(targetId);
                })
                .orElseGet(TraceRecordResponseDto::hidden);
    }
}