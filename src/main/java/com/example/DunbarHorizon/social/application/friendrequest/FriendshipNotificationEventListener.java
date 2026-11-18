package com.example.DunbarHorizon.social.application.friendrequest;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.social.application.friendship.port.FriendshipQueryRepository;
import com.example.DunbarHorizon.social.domain.friendship.event.FriendRequestAcceptedEvent;
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
    private final FriendshipQueryRepository friendshipQueryRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        friendshipQueryRepository.findFriend(event.requesterId(), event.receiverId())
                .ifPresent(friend -> eventPublisher.publishEvent(new NotificationEvent(
                        event.requesterId(),
                        "친구 수락",
                        friend.nickname() + "님과 이제 친구입니다.",
                        NotificationType.FRIEND_REQUEST_ACCEPT,
                        Map.of("friendId", friend.friendId(), "friendName", friend.nickname())
                )));
    }
}
