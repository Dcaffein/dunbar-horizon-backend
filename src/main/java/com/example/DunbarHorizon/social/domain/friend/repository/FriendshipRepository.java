package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FriendshipRepository {
    Friendship save(Friendship friendship);
    Optional<Friendship> findById(String id);
    void delete(String friendshipId);

    boolean existsFriendshipBetween(Long userId, Long targetId);

    List<Friendship> findByUserId(Long userId);

    Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted);

    Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds);

    void applyDecay(double rate, double threshold, LocalDateTime decayTime);
    void updateUserFields(Friendship friendship, Long userId);
    void incrementInterestScore(String friendshipId, Long userId, Long friendId, double delta);
    void incrementMutualInterestScore(String friendshipId, Long userAId, Long userBId, double delta);
    void incrementMutualInterestScoresBatch(List<MutualInterestScoreUpdate> updates);
    List<FriendshipArchiveCandidate> findArchiveCandidates(double threshold);
    void deleteAllByIds(Collection<String> ids);
}
