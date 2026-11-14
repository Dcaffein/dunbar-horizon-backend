package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.exceptions.TransientException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.List;

@RequiredArgsConstructor
public class InteractionScoreService {

    private final FriendshipRepository friendshipRepository;

    @Retryable(retryFor = {TransientException.class, TransientDataAccessException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true))
    @Neo4jTransactional
    public void applyInteraction(Long userAId, Long userBId, InteractionType type) {
        FriendshipScoreUpdater.applyInteraction(friendshipRepository, userAId, userBId, type);
    }

    @Retryable(retryFor = {TransientException.class, TransientDataAccessException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true))
    @Neo4jTransactional
    public void applyFlagConclusion(Long flagId, Long hostId, List<Long> participantIds,
                                    InteractionType type, ScoreDirection direction) {
        FriendshipScoreUpdater.applyFlagConclusion(friendshipRepository, hostId, participantIds, type, direction);
    }
}
