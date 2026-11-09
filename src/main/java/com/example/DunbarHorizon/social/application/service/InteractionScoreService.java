package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.exceptions.TransientException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InteractionScoreService {

    private final FriendshipRepository friendshipRepository;

    @Retryable(retryFor = {TransientException.class, TransientDataAccessException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true))
    @Neo4jTransactional
    public void applyInteraction(Long userAId, Long userBId, InteractionType type) {
        double delta = InteractionScorePolicy.scoreOf(type);
        List<Friendship> friendships = friendshipRepository.findAllByIdsForUpdate(
                Set.of(Friendship.generateCompositeId(userAId, userBId)));

        for (Friendship friendship : friendships) {
            if (type.isMutual()) {
                friendship.adjustMutualInterestScore(delta);
            } else {
                friendship.adjustInterestScore(userAId, delta);
            }
        }
        friendshipRepository.saveInterestScores(friendships);
    }

    @Retryable(retryFor = {TransientException.class, TransientDataAccessException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 50, maxDelay = 200, random = true))
    @Neo4jTransactional
    public void applyFlagConclusion(Long flagId, Long hostId, List<Long> participantIds,
                                    InteractionType type, ScoreDirection direction) {
        double delta = direction.signed(InteractionScorePolicy.scoreOf(type));
        List<Friendship> friendships = friendshipRepository.findAllByIdsForUpdate(flagPairIds(hostId, participantIds));

        friendships.forEach(friendship -> friendship.adjustMutualInterestScore(delta));
        friendshipRepository.saveInterestScores(friendships);
    }

    private static Set<String> flagPairIds(Long hostId, List<Long> participantIds) {
        Set<String> ids = new LinkedHashSet<>();
        for (Long participantId : participantIds) {
            ids.add(Friendship.generateCompositeId(hostId, participantId));
        }
        for (int i = 0; i < participantIds.size(); i++) {
            for (int j = i + 1; j < participantIds.size(); j++) {
                ids.add(Friendship.generateCompositeId(participantIds.get(i), participantIds.get(j)));
            }
        }
        return ids;
    }
}
