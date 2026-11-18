package com.example.DunbarHorizon.social.application.friendship.port;

import com.example.DunbarHorizon.social.domain.friendship.Friendship;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FriendshipScoreRepository {
    Optional<Friendship> findBetweenForUpdate(Long userId, Long friendId);

    List<Friendship> findAllAmongForUpdate(Collection<Long> userIds);

    void saveInterestScores(Collection<Friendship> friendships);

    void applyDecay(double rate, double threshold, LocalDateTime decayTime);
}
