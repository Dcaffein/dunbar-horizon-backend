package com.example.GooRoomBe.social.trace.application;

import com.example.GooRoomBe.global.event.NotificationEvent;
import com.example.GooRoomBe.global.event.NotificationType;
import com.example.GooRoomBe.social.trace.domain.event.TraceRevealedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TraceEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTraceRevealed(TraceRevealedEvent event) {
        eventPublisher.publishEvent(new NotificationEvent(
                event.targetId(),
                "서로간 잦은 방문",
                String.format("%s님과 서로 통했습니다! 접속해서 인사를 건네보세요", event.visitorNickname()),
                "/api/v1/users/" + event.visitorId() + "/profile",
                NotificationType.TRACE_REVEALED
        ));
    }
}