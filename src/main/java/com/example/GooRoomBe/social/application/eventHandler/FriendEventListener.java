package com.example.GooRoomBe.social.application.eventHandler;

import com.example.GooRoomBe.global.event.notification.NotificationEvent;
import com.example.GooRoomBe.global.event.notification.NotificationType;
import com.example.GooRoomBe.social.domain.friend.FriendRequest;
import com.example.GooRoomBe.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.GooRoomBe.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.GooRoomBe.social.domain.friend.repository.FriendRequestRepository;
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

    @EventListener
    @Transactional
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {

        FriendRequest request = friendRequestRepository.findById(event.requestId())
                .orElseThrow(()->new FriendRequestNotFoundException(event.requestId()));
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
        request.complete();
        friendRequestRepository.saveRequest(request);
    }
}