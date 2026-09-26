package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.exceptions.TransientException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntimacyScoreManager {

    private static final int FLAG_UPDATE_MAX_ATTEMPTS = 3;

    private final FriendshipRepository friendshipRepository;
    private final FriendshipDecayPolicy decayPolicy;
    private final ExecutorService scoreMutationExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "intimacy-score-manager");
        thread.setDaemon(false);
        return thread;
    });

    public void enqueueFlagConclusion(Long flagId, Long hostId, List<Long> participantIds, double delta) {
        List<Long> queuedParticipantIds = List.copyOf(participantIds);
        scoreMutationExecutor.execute(() -> applyFlagConclusion(flagId, hostId, queuedParticipantIds, delta));
    }

    public void enqueueDecay() {
        scoreMutationExecutor.execute(this::applyDecay);
    }

    void applyFlagConclusion(Long flagId, Long hostId, List<Long> participantIds, double delta) {
        List<MutualInterestScoreUpdate> updates = createFlagUpdates(hostId, participantIds, delta);

        for (int attempt = 1; attempt <= FLAG_UPDATE_MAX_ATTEMPTS; attempt++) {
            try {
                friendshipRepository.incrementMutualInterestScoresBatch(updates);
                log.debug("Flag intimacy scores updated: flagId={}, participants={}, pairs={}",
                        flagId, participantIds.size(), updates.size());
                return;
            } catch (RuntimeException exception) {
                if (!isTransientFailure(exception) || attempt == FLAG_UPDATE_MAX_ATTEMPTS) {
                    log.error("Flag intimacy score update failed: flagId={}, participants={}, pairs={}, attempts={}",
                            flagId, participantIds.size(), updates.size(), attempt, exception);
                    return;
                }
                log.warn("Retrying transient flag intimacy score update: flagId={}, participants={}, pairs={}, attempt={}",
                        flagId, participantIds.size(), updates.size(), attempt, exception);
            }
        }
    }

    void applyDecay() {
        try {
            double rate = decayPolicy.getDecayRate();
            double minThreshold = decayPolicy.getMinThreshold();
            LocalDateTime thresholdTime = decayPolicy.getDecayThresholdTime();
            friendshipRepository.applyDecay(rate, minThreshold, thresholdTime);
        } catch (RuntimeException exception) {
            log.error("Failed to apply intimacy score decay", exception);
        }
    }

    private List<MutualInterestScoreUpdate> createFlagUpdates(Long hostId, List<Long> participantIds, double delta) {
        List<MutualInterestScoreUpdate> updates = new ArrayList<>();

        for (Long participantId : participantIds) {
            updates.add(mutualUpdate(hostId, participantId, delta));
        }
        for (int i = 0; i < participantIds.size(); i++) {
            for (int j = i + 1; j < participantIds.size(); j++) {
                updates.add(mutualUpdate(participantIds.get(i), participantIds.get(j), delta));
            }
        }
        return updates;
    }

    private MutualInterestScoreUpdate mutualUpdate(Long userAId, Long userBId, double delta) {
        return new MutualInterestScoreUpdate(
                Friendship.generateCompositeId(userAId, userBId), userAId, userBId, delta
        );
    }

    private boolean isTransientFailure(RuntimeException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof TransientException || cause instanceof TransientDataAccessException) {
                return true;
            }
        }
        return false;
    }

    @PreDestroy
    void shutdown() {
        scoreMutationExecutor.shutdown();
        try {
            if (!scoreMutationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scoreMutationExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            scoreMutationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
