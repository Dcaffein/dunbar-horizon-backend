package com.example.DunbarHorizon.buzz.application.eventHandler;

import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.event.BuzzRepliedEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class BuzzNotificationDispatcher {
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
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

    @EventListener
    public void dispatch(BuzzRepliedEvent event) {
        NotificationEvent notification = NotificationEvent.builder()
                .receiverId(event.creatorId())
                .title("Buzz 답장")
                .content("누군가 내 Buzz에 응답했습니다")
                .type(NotificationType.BUZZ_RESPONSE)
                .metadata(Map.of(
                        "buzzId", event.buzzId(),
                        "replierId", event.replierId()
                ))
                .build();

        eventPublisher.publishEvent(notification);
    }
}
