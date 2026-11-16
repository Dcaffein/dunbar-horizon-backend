package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.domain.friend.Friendship;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FriendshipRepository {
    void create(Friendship friendship);

    Optional<Friendship> findBetween(Long userId, Long friendId);

    Optional<Friendship> findBetweenForUpdate(Long userId, Long friendId);

    List<Friendship> findAllBetweenForUpdate(Long userId, Collection<Long> friendIds);

    List<Friendship> findAllAmongForUpdate(Collection<Long> userIds);

    boolean existsFriendshipBetween(Long userId, Long friendId);

    boolean existsFriendshipForUpdate(Long userId, Long friendId);

    List<Friendship> findByUserId(Long userId);

    Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted);

    Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds);

    void updateUserFields(Friendship friendship, Long userId);

    void saveInterestScores(Collection<Friendship> friendships);

    void applyDecay(double rate, double threshold, LocalDateTime decayTime);

    boolean deleteBetween(Long userId, Long friendId);
}
