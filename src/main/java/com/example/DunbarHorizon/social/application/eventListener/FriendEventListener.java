package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FriendEventListener {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipBroker friendshipBroker;
    private final FriendshipRepository friendshipRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        FriendRequest request = friendRequestRepository.findById(event.requestId())
                .orElseThrow(() -> new FriendRequestNotFoundException(event.requestId()));

        Friendship friendship = friendshipBroker.establish(request);
        friendshipRepository.save(friendship);
    }
}
