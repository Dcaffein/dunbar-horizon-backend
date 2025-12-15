package com.example.GooRoomBe.social.friend.application;

import com.example.GooRoomBe.global.event.UserInteractionEvent;
import com.example.GooRoomBe.social.friend.domain.Friendship;
import com.example.GooRoomBe.social.friend.domain.FriendshipPort; // 혹은 Repository
import com.example.GooRoomBe.social.friend.exception.FriendshipNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendInteractionEventListener {

    private final FriendshipPort friendshipPort;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserInteraction(UserInteractionEvent event) {
        try {
            Friendship friendship = friendshipPort.getFriendship(event.actorId(), event.targetId());

            friendship.adjustInterestScore(event.actorId(), event.score());

            friendshipPort.save(friendship);

            log.debug("Interest score adjusted: {} -> {}, delta={}", event.actorId(), event.targetId(), event.score());

        } catch (FriendshipNotFoundException e) {
            // friendship 없으면 무시
        } catch (Exception e) {
            log.error("Failed to handle interaction event", e);
        }
    }
}