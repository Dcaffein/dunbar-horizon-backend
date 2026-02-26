package com.example.GooRoomBe.cast.application.eventHandler;

import com.example.GooRoomBe.cast.domain.event.CastCreatedEvent;
import com.example.GooRoomBe.cast.domain.event.CastRepliedEvent;
import com.example.GooRoomBe.global.event.notification.NotificationEvent;
import com.example.GooRoomBe.global.event.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CastNotificationDispatcher {
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void dispatch(CastCreatedEvent event) {
        event.recipientIds().forEach(receiverId -> {
            NotificationEvent notification = new NotificationEvent(
                    receiverId,"새로운 캐스트","친구로부터 새로운 캐스트가 도착했습니다!",NotificationType.CAST_ARRIVAL,
                    Map.of(
                            "castId", event.castId(),
                            "senderId", event.creatorId()
                    )
            );

            eventPublisher.publishEvent(notification);
        });
    }

    @EventListener
    public void dispatch(CastRepliedEvent event) {
        NotificationEvent notification = NotificationEvent.builder()
                .receiverId(event.creatorId())
                .title("캐스트 답장")
                .content("누군가 내 캐스트에 답장을 남겼습니다.")
                .type(NotificationType.CAST_RESPONSE)
                .metadata(Map.of(
                        "castId", event.castId(),
                        "replierId", event.replierId()
                ))
                .build();

        eventPublisher.publishEvent(notification);
    }
}
