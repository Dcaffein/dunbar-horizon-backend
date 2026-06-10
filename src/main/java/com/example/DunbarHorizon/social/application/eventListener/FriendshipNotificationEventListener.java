package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FriendshipNotificationEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        eventPublisher.publishEvent(new NotificationEvent(
                event.requesterId(),
                "친구 수락",
                event.receiverNickname() + "님과 이제 친구입니다.",
                NotificationType.FRIEND_REQUEST_ACCEPT,
                Map.of(
                        "friendId", event.receiverId(),
                        "friendName", event.receiverNickname()
                )
        ));
    }
}
