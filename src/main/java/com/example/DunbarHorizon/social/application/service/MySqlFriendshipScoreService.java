package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MySqlFriendshipScoreService {

    private final FriendshipRepository friendships;
    private final FriendshipDecayPolicy decayPolicy;

    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true))
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public void applyInteraction(Long userAId, Long userBId, InteractionType type) {
        FriendshipScoreUpdater.applyInteraction(friendships, userAId, userBId, type);
    }

    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true))
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public void applyFlagConclusion(Long hostId, List<Long> participantIds, InteractionType type, ScoreDirection direction) {
        FriendshipScoreUpdater.applyFlagConclusion(friendships, hostId, participantIds, type, direction);
    }

    @Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
    public void processDecay() {
        friendships.applyDecay(decayPolicy.getDecayRate(), decayPolicy.getMinThreshold(),
                decayPolicy.getDecayThresholdTime());
    }
}
