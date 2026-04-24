package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.port.in.FriendRequestReceiverActionUseCase;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendRequestReceiverActionService implements FriendRequestReceiverActionUseCase {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipBroker friendshipBroker;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void acceptRequest(String requestId, Long receiverId) {
        FriendRequest request = findRequestById(requestId);

        request.accept(receiverId);
        friendshipBroker.establish(request);
        friendRequestRepository.deleteById(requestId);

        eventPublisher.publishEvent(new FriendRequestAcceptedEvent(
                request.getId(),
                request.getRequester().getId(),
                request.getReceiver().getId(),
                request.getReceiver().getNickname()
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
