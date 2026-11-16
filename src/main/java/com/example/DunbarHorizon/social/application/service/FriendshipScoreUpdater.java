package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class FriendshipScoreUpdater {

    private FriendshipScoreUpdater() {
    }

    static void applyInteraction(FriendshipRepository repository, Long userAId, Long userBId, InteractionType type) {
        double delta = InteractionScorePolicy.scoreOf(type);
        List<Friendship> friendships = repository.findBetweenForUpdate(userAId, userBId).stream().toList();

        for (Friendship friendship : friendships) {
            if (type.isMutual()) {
                friendship.adjustMutualInterestScore(delta);
            } else {
                friendship.adjustInterestScore(userAId, delta);
            }
        }
        repository.saveInterestScores(friendships);
    }

    static void applyFlagConclusion(FriendshipRepository repository, Long hostId, List<Long> participantIds,
                                    InteractionType type, ScoreDirection direction) {
        double delta = direction.signed(InteractionScorePolicy.scoreOf(type));
        Set<Long> members = new LinkedHashSet<>();
        members.add(hostId);
        members.addAll(participantIds);
        List<Friendship> friendships = repository.findAllAmongForUpdate(members);
        friendships.forEach(friendship -> friendship.adjustMutualInterestScore(delta));
        repository.saveInterestScores(friendships);
    }
}
