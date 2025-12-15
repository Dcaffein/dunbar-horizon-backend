package com.example.GooRoomBe.social.trace.application;

import com.example.GooRoomBe.social.common.dto.SocialMemberResponseDto;
import com.example.GooRoomBe.social.socialUser.SocialUser;
import com.example.GooRoomBe.social.socialUser.SocialUserPort;
import com.example.GooRoomBe.social.trace.api.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.social.trace.domain.Trace;
import com.example.GooRoomBe.social.trace.domain.TracePort;
import com.example.GooRoomBe.social.trace.domain.event.TraceRevealedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TraceService {

    private final TracePort tracePort;
    private final SocialUserPort socialUserPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TraceRecordResponseDto visit(String visitorId, String targetId) {
        if (visitorId.equals(targetId)) {
            return TraceRecordResponseDto.hidden();
        }

        return tracePort.findTrace(visitorId, targetId)
                .map(trace -> handleRevisit(trace, visitorId, targetId))
                .orElseGet(() -> handleNewVisit(visitorId, targetId));
    }

    private TraceRecordResponseDto handleNewVisit(String visitorId, String targetId) {
        SocialUser visitor = socialUserPort.getUser(visitorId);
        SocialUser target = socialUserPort.getUser(targetId);

        Trace newTrace = new Trace(visitor, target);

        tracePort.save(newTrace, visitorId);
        eventPublisher.publishEvent(newTrace.createInteractionEvent());

        return TraceRecordResponseDto.hidden();
    }

    private TraceRecordResponseDto handleRevisit(Trace trace, String visitorId, String targetId) {
        trace.updateVisitCount();
        tracePort.save(trace, visitorId);
        eventPublisher.publishEvent(trace.createInteractionEvent());

        if (!trace.isRevealReady()) {
            return TraceRecordResponseDto.hidden();
        }

        int partnerCount = tracePort.getVisitCount(targetId, visitorId);
        Optional<TraceRevealedEvent> revealEvent = trace.checkRevealEvent(partnerCount);

        if (revealEvent.isPresent()) {
            eventPublisher.publishEvent(revealEvent.get());
            return TraceRecordResponseDto.revealed(SocialMemberResponseDto.from(trace.getTarget()));
        }

        return TraceRecordResponseDto.hidden();
    }
}