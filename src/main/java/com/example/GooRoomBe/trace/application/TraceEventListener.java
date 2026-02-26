package com.example.GooRoomBe.trace.application;

import com.example.GooRoomBe.global.event.notification.NotificationEvent;
import com.example.GooRoomBe.global.event.notification.NotificationType;
import com.example.GooRoomBe.trace.domain.event.TraceRevealedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TraceEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTraceRevealed(TraceRevealedEvent event) {
        Map<String,Object> metadata = new HashMap<>();

        eventPublisher.publishEvent(new NotificationEvent(
                event.targetId(),
                "서로간 잦은 방문",
                "서로 통했습니다! 접속해서 인사를 건네보세요",
                NotificationType.TRACE_REVEALED,
                metadata
        ));
    }
}