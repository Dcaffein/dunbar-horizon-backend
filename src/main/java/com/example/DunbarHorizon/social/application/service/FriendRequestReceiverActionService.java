package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.social.application.port.in.FriendRequestReceiverActionUseCase;
import com.example.DunbarHorizon.social.domain.friend.event.FriendshipCreatedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Neo4jTransactional
public class FriendRequestReceiverActionService implements FriendRequestReceiverActionUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipBroker friendshipBroker;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void acceptRequest(String requestId, Long receiverId) {
        FriendRequest request = findRequestById(requestId);
        request.accept(receiverId);

        Friendship friendship = friendshipBroker.createFrom(request);
        friendshipRepository.save(friendship);
        friendRequestRepository.deleteById(requestId);

        eventPublisher.publishEvent(new FriendshipCreatedEvent(
                request.getRequester().getId(),
                request.getReceiver().getId()
        ));
        eventPublisher.publishEvent(new NotificationEvent(
                request.getRequester().getId(),
                "친구 수락",
                request.getReceiver().getNickname() + "님과 이제 친구입니다.",
                NotificationType.FRIEND_REQUEST_ACCEPT,
                Map.of(
                        "friendId", request.getReceiver().getId(),
                        "friendName", request.getReceiver().getNickname()
                )
        ));
    }

    @Override
    public void hideRequest(String requestId, Long receiverId) {
        FriendRequest request = findRequestById(requestId);
        request.hide(receiverId);
        friendRequestRepository.saveRequest(request);
    }

    @Override
    public void undoHideRequest(String requestId, Long receiverId) {
        FriendRequest request = findRequestById(requestId);
        request.undoHide(receiverId);
        friendRequestRepository.saveRequest(request);
    }

    private FriendRequest findRequestById(String requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));
    }
}
