package com.example.DunbarHorizon.social.application.eventHandler;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FriendEventListener {
    private final ApplicationEventPublisher eventPublisher;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipBroker friendshipBroker;
    private final FriendshipRepository friendshipRepository;

    @EventListener
    @Transactional
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {

        FriendRequest request = friendRequestRepository.findById(event.requestId())
                .orElseThrow(() -> new FriendRequestNotFoundException(event.requestId()));

        Friendship friendship = friendshipBroker.establish(request);
        friendshipRepository.save(friendship);

        NotificationEvent notification = new NotificationEvent(
                event.requesterId(),
                "친구 수락",
                event.receiverNickname() + "님과 이제 친구입니다.",
                NotificationType.FRIEND_REQUEST_ACCEPT,
                Map.of(
                        "friendId", event.receiverId(),
                        "friendName", event.receiverNickname()
                )
        );

        eventPublisher.publishEvent(notification);
    }
}