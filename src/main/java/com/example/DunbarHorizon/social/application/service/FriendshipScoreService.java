package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.application.port.out.FriendshipScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FriendshipScoreService {

    private final FriendshipScoreRepository friendships;
    private final FriendshipDecayPolicy decayPolicy;

    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true))
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public void applyInteraction(Long userAId, Long userBId, InteractionType type) {
        double delta = InteractionScorePolicy.scoreOf(type);
        List<Friendship> locked = friendships.findBetweenForUpdate(userAId, userBId).stream().toList();
        for (Friendship friendship : locked) {
            if (type.isMutual()) {
                friendship.adjustMutualInterestScore(delta);
            } else {
                friendship.adjustInterestScore(userAId, delta);
            }
        }
        friendships.saveInterestScores(locked);
    }

    @Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true))
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public void applyFlagConclusion(Long hostId, List<Long> participantIds, InteractionType type, ScoreDirection direction) {
        double delta = direction.signed(InteractionScorePolicy.scoreOf(type));
        Set<Long> members = new LinkedHashSet<>();
        members.add(hostId);
        members.addAll(participantIds);
        List<Friendship> locked = friendships.findAllAmongForUpdate(members);
        locked.forEach(friendship -> friendship.adjustMutualInterestScore(delta));
        friendships.saveInterestScores(locked);
    }

    @Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
    public void processDecay() {
        friendships.applyDecay(decayPolicy.getDecayRate(), decayPolicy.getMinThreshold(),
                decayPolicy.getDecayThresholdTime());
    }
}
