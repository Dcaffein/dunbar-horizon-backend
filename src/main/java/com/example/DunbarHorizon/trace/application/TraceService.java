package com.example.DunbarHorizon.trace.application;

import com.example.DunbarHorizon.trace.adapter.in.web.dto.TraceRecordResponseDto;
import com.example.DunbarHorizon.trace.domain.model.Trace;
import com.example.DunbarHorizon.trace.domain.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceRepository traceRepository;

    @Transactional
    public TraceRecordResponseDto recordTrace(Long visitorId, Long targetId) {

        Long userAId = Math.min(visitorId, targetId);
        Long userBId = Math.max(visitorId, targetId);

        Trace trace = traceRepository.findByUserAIdAndUserBId(userAId, userBId)
                .orElseGet(() -> new Trace(visitorId, targetId));

        trace.recordVisit(visitorId);

        traceRepository.save(trace);

        if (trace.isRevealed()) {
            return TraceRecordResponseDto.revealed(targetId);
        }
        return TraceRecordResponseDto.hidden();
    }
}