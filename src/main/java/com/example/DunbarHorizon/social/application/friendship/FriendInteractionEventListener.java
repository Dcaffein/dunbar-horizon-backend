package com.example.DunbarHorizon.social.application.friendship;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FriendInteractionEventListener {

    private final FriendshipScoreService friendshipScores;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserInteraction(UserInteractionEvent event) {
        friendshipScores.applyInteraction(event.userA(), event.userB(), event.type());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBatchMutualInteraction(BatchMutualInteractionEvent event) {
        friendshipScores.applyFlagConclusion(event.hostId(), event.participantIds(), event.type(), event.direction());
    }
}
