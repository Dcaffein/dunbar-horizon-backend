package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.MutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.application.port.out.InteractionScoreDeltaPort;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendInteractionEventListener {

    private final InteractionScoreDeltaPort deltaPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserInteraction(UserInteractionEvent event) {
        try {
            String friendshipId = Friendship.generateCompositeId(event.actorId(), event.targetId());
            double delta = InteractionScorePolicy.scoreOf(event.type());
            deltaPort.accumulate(friendshipId, event.actorId(), delta);
            log.debug("Interaction delta buffered: {} -> {}, type={}", event.actorId(), event.targetId(), event.type());
        } catch (Exception e) {
            log.error("Failed to buffer interaction delta: {} -> {}", event.actorId(), event.targetId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMutualInteraction(MutualInteractionEvent event) {
        try {
            String friendshipId = Friendship.generateCompositeId(event.userIdA(), event.userIdB());
            double delta = InteractionScorePolicy.scoreOf(event.type());
            deltaPort.accumulate(friendshipId, event.userIdA(), delta);
            deltaPort.accumulate(friendshipId, event.userIdB(), delta);
            log.debug("Mutual delta buffered: {} <-> {}, type={}", event.userIdA(), event.userIdB(), event.type());
        } catch (Exception e) {
            log.error("Failed to buffer mutual interaction delta: {} <-> {}", event.userIdA(), event.userIdB(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBatchMutualInteraction(BatchMutualInteractionEvent event) {
        try {
            double delta = InteractionScorePolicy.scoreOf(event.type());
            List<Long> participantIds = event.participantIds();

            participantIds.forEach(participantId -> {
                String friendshipId = Friendship.generateCompositeId(event.hostId(), participantId);
                deltaPort.accumulate(friendshipId, event.hostId(), delta);
                deltaPort.accumulate(friendshipId, participantId, delta);
            });

            for (int i = 0; i < participantIds.size(); i++) {
                for (int j = i + 1; j < participantIds.size(); j++) {
                    String friendshipId = Friendship.generateCompositeId(participantIds.get(i), participantIds.get(j));
                    deltaPort.accumulate(friendshipId, participantIds.get(i), delta);
                    deltaPort.accumulate(friendshipId, participantIds.get(j), delta);
                }
            }
            log.debug("Batch mutual delta buffered: host={}, participants={}, type={}", event.hostId(), participantIds.size(), event.type());
        } catch (Exception e) {
            log.error("Failed to buffer batch mutual interaction delta: host={}", event.hostId(), e);
        }
    }
}
