package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.application.service.InteractionScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FriendInteractionEventListener {

    private final InteractionScoreService interactionScoreService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserInteraction(UserInteractionEvent event) {
        interactionScoreService.applyInteraction(event.userA(), event.userB(), event.type());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBatchMutualInteraction(BatchMutualInteractionEvent event) {
        interactionScoreService.applyFlagConclusion(
                event.flagId(), event.hostId(), event.participantIds(), event.type(), event.direction());
    }
}
