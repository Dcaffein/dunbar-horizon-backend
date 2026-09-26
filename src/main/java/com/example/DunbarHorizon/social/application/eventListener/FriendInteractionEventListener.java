package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.application.service.IntimacyScoreManager;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendInteractionEventListener {

    private final FriendshipRepository friendshipRepository;
    private final IntimacyScoreManager intimacyScoreManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserInteraction(UserInteractionEvent event) {
        try {
            String friendshipId = Friendship.generateCompositeId(event.userA(), event.userB());
            double delta = InteractionScorePolicy.scoreOf(event.type());

            if (event.type().isMutual()) {
                friendshipRepository.incrementMutualInterestScore(
                        friendshipId, event.userA(), event.userB(), delta
                );
            } else {
                friendshipRepository.incrementInterestScore(
                        friendshipId, event.userA(), event.userB(), delta
                );
            }
            log.debug("Interaction score updated: {} <-> {}, type={}", event.userA(), event.userB(), event.type());
        } catch (Exception e) {
            log.error("Failed to update interaction score: {} <-> {}", event.userA(), event.userB(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBatchMutualInteraction(BatchMutualInteractionEvent event) {
        double delta = InteractionScorePolicy.scoreOf(event.type());
        intimacyScoreManager.enqueueFlagConclusion(event.flagId(), event.hostId(), event.participantIds(), delta);
    }
}
