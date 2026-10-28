package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.exceptions.TransientException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionScoreService {

    private static final int MAX_ATTEMPTS = 3;

    private final FriendshipRepository friendshipRepository;

    public void applyInteraction(Long userAId, Long userBId, InteractionType type) {
        String friendshipId = Friendship.generateCompositeId(userAId, userBId);
        double delta = InteractionScorePolicy.scoreOf(type);

        write(
                () -> {
                    if (type.isMutual()) {
                        friendshipRepository.incrementMutualInterestScore(friendshipId, userAId, userBId, delta);
                    } else {
                        friendshipRepository.incrementInterestScore(friendshipId, userAId, userBId, delta);
                    }
                },
                () -> "%d <-> %d, type=%s".formatted(userAId, userBId, type)
        );
    }

    public void applyFlagConclusion(Long flagId, Long hostId, List<Long> participantIds, InteractionType type) {
        double delta = InteractionScorePolicy.scoreOf(type);
        List<MutualInterestScoreUpdate> updates = createFlagUpdates(hostId, participantIds, delta);

        write(
                () -> friendshipRepository.incrementMutualInterestScoresBatch(updates),
                () -> "flagId=%d, participants=%d, pairs=%d".formatted(flagId, participantIds.size(), updates.size())
        );
    }

    private void write(Runnable command, Supplier<String> target) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                command.run();
                log.debug("Interaction score updated: {}", target.get());
                return;
            } catch (RuntimeException exception) {
                if (!isTransientFailure(exception) || attempt == MAX_ATTEMPTS) {
                    log.error("Interaction score update failed: {}, attempts={}", target.get(), attempt, exception);
                    throw exception;
                }
                log.warn("Retrying transient interaction score update: {}, attempt={}", target.get(), attempt, exception);
            }
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
}
