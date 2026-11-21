package com.example.DunbarHorizon.buzz.application.eventListener;

import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.event.BuzzThreadOpenedEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
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
                    receiverId, "새로운 Buzz", "응답해보세요", NotificationType.BUZZ_ARRIVAL,
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
    public void dispatch(BuzzThreadOpenedEvent event) {
        eventPublisher.publishEvent(new NotificationEvent(
                event.notificationReceiverId(), "Buzz 응답", "누군가 내 Buzz에 응답했습니다",
                NotificationType.BUZZ_RESPONSE,
                Map.of(
                        "buzzId", event.buzzId(),
                        "threadId", event.threadId(),
                        "authorId", event.starterId()
                )
        ));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(BuzzCommentedEvent event) {
        eventPublisher.publishEvent(new NotificationEvent(
                List.copyOf(event.notificationReceiverIds()), "Buzz 답글", "스레드에 새 답글이 달렸습니다",
                NotificationType.BUZZ_RESPONSE,
                Map.of(
                        "buzzId", event.buzzId(),
                        "threadId", event.threadId(),
                        "authorId", event.authorId()
                ),
                null
        ));
    }
}
