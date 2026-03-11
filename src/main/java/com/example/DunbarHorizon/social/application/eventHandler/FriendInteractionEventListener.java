package com.example.DunbarHorizon.social.application.eventHandler;

import com.example.DunbarHorizon.global.event.interaction.MutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.exceptions.TransientException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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

    private final FriendshipRepository friendshipRepository;

    @Async
    @Retryable(
            retryFor = {
                    OptimisticLockingFailureException.class,
                    DataAccessResourceFailureException.class,
                    TransientException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserInteraction(UserInteractionEvent event) {
        String friendshipId = Friendship.generateCompositeId(event.actorId(), event.targetId());
        friendshipRepository.findById(friendshipId).ifPresent(friendship -> {
            friendship.adjustInterestScore(event.actorId(), InteractionScorePolicy.scoreOf(event.type()));
            friendshipRepository.save(friendship);
            log.debug("Interest score adjusted: {} -> {}, type={}", event.actorId(), event.targetId(), event.type());
        });
    }

    @Recover
    public void recover(Exception e, UserInteractionEvent event) {
        log.error("Failed to handle interaction event after retries: {} -> {}", event.actorId(), event.targetId(), e);
    }

    @Async
    @Retryable(
            retryFor = {
                    OptimisticLockingFailureException.class,
                    DataAccessResourceFailureException.class,
                    TransientException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMutualInteraction(MutualInteractionEvent event) {
        String friendshipId = Friendship.generateCompositeId(event.userIdA(), event.userIdB());
        friendshipRepository.findById(friendshipId).ifPresent(friendship -> {
            friendship.adjustInterestScore(event.userIdA(), InteractionScorePolicy.scoreOf(event.type()));
            friendship.adjustInterestScore(event.userIdB(), InteractionScorePolicy.scoreOf(event.type()));
            friendshipRepository.save(friendship);
            log.debug("Mutual interest score adjusted: {} <-> {}, type={}", event.userIdA(), event.userIdB(), event.type());
        });
    }

    @Recover
    public void recover(Exception e, MutualInteractionEvent event) {
        log.error("Failed to handle mutual interaction event after retries: {} <-> {}", event.userIdA(), event.userIdB(), e);
    }
}