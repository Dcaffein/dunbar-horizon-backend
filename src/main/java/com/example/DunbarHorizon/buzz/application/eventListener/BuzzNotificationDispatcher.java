package com.example.DunbarHorizon.buzz.application.eventListener;

import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BuzzNotificationDispatcher {
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(BuzzCreatedEvent event) {
        event.recipientIds().forEach(receiverId -> {
            NotificationEvent notification = new NotificationEvent(
                    receiverId,"새로운 Buzz","응답해보세요",NotificationType.BUZZ_ARRIVAL,
                    Map.of(
                            "buzzId", event.buzzId(),
                            "creatorId", event.creatorId()
                    )
            );

            eventPublisher.publishEvent(notification);
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(BuzzCommentedEvent event) {
        if (event.creatorId().equals(event.commenterId())) return;
        NotificationEvent notification = NotificationEvent.builder()
                .receiverId(event.creatorId())
                .title("Buzz 댓글")
                .content("누군가 내 Buzz에 댓글을 남겼습니다")
                .type(NotificationType.BUZZ_RESPONSE)
                .metadata(Map.of(
                        "buzzId", event.buzzId(),
                        "commenterId", event.commenterId()
                ))
                .build();

        eventPublisher.publishEvent(notification);
    }
}
