package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.application.service.MySqlFriendshipScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FriendInteractionEventListener {

    private final MySqlFriendshipScoreService friendshipScores;

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
