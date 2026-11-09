package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FriendshipRepository {
    void create(Friendship friendship);
    Optional<Friendship> findById(String id);
    void delete(String friendshipId);

    boolean existsFriendshipBetween(Long userId, Long targetId);

    List<Friendship> findByUserId(Long userId);

    Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted);

    Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds);

    void applyDecay(double rate, double threshold, LocalDateTime decayTime);
    void updateUserFields(Friendship friendship, Long userId);
    List<Friendship> findAllByIdsForUpdate(Collection<String> ids);
    void saveInterestScores(Collection<Friendship> friendships);
    List<FriendshipArchiveCandidate> findArchiveCandidates(double threshold);
    void deleteAllByIds(Collection<String> ids);
}
