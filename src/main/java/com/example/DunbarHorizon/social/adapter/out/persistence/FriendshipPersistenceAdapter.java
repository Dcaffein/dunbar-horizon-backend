package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ActiveUserJpaRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.FriendshipJpaRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipScoreRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelMemberFriendshipLookup;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipConflictException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FriendshipPersistenceAdapter
        implements FriendshipRepository, FriendshipScoreRepository, LabelMemberFriendshipLookup {

    private static final String PAIR_KEY = "PRIMARY";

    private final FriendshipJpaRepository friendshipJpaRepository;
    private final FriendshipJdbcRepository friendshipJdbcRepository;
    private final ActiveUserJpaRepository activeUserJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void create(Friendship friendship) {
        try {
            entityManager.persist(friendship);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (e instanceof EntityExistsException || PersistenceErrors.violates(e, PAIR_KEY)) {
                throw new AlreadyFriendsException(friendship.getLower().getOwnerId(), friendship.getHigher().getOwnerId());
            }
            throw e;
        }
    }

    @Override
    public Optional<Friendship> findBetween(Long userId, Long friendId) {
        var key = FriendshipKey.of(userId, friendId);
        return friendshipJpaRepository.findBetweenActiveUsers(key.low(), key.high());
    }

    @Override
    public void saveSettings(Friendship friendship) {
        try {
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw new FriendshipConflictException(friendship.getLower().getOwnerId(), friendship.getHigher().getOwnerId());
        }
    }

    @Override
    public boolean existsBetween(Long userId, Long friendId) {
        var key = FriendshipKey.of(userId, friendId);
        return friendshipJpaRepository.existsBetweenActiveUsers(key.low(), key.high());
    }

    @Override
    public boolean deleteBetween(Long userId, Long friendId) {
        var key = FriendshipKey.of(userId, friendId);
        return friendshipJpaRepository.deleteBetweenActiveUsers(key.low(), key.high()) > 0;
    }

    @Override
    public Optional<Friendship> findBetweenForUpdate(Long userId, Long friendId) {
        return friendshipJdbcRepository.findForUpdate(FriendshipKey.of(userId, friendId));
    }

    @Override
    public Candidates findCandidatesWithSharedLock(Long ownerId, Collection<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return new Candidates(Set.of(), Set.of());
        }
        Set<Long> requestedIds = new HashSet<>(candidateIds);
        Set<Long> userIds = new HashSet<>(requestedIds);
        userIds.add(ownerId);
        Set<Long> activeUserIds = activeUserJpaRepository.findActiveIds(userIds);
        Set<Long> activeCandidateIds = activeUserIds.stream()
                .filter(requestedIds::contains)
                .collect(Collectors.toUnmodifiableSet());
        List<Long> activeOtherIds = activeCandidateIds.stream()
                .filter(candidateId -> !ownerId.equals(candidateId))
                .toList();
        if (!activeUserIds.contains(ownerId) || activeOtherIds.isEmpty()) {
            return new Candidates(activeCandidateIds, Set.of());
        }
        Set<Long> lockedFriendIds = friendshipJpaRepository.findAllBetweenForShare(ownerId, activeOtherIds).stream()
                .map(friendship -> friendship.getFriendId(ownerId))
                .collect(Collectors.toUnmodifiableSet());
        return new Candidates(activeCandidateIds, lockedFriendIds);
    }

    @Override
    public List<Friendship> findAllAmongForUpdate(Collection<Long> userIds) {
        List<Long> members = userIds.stream().filter(Objects::nonNull).distinct().toList();
        List<FriendshipKey> keys = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                keys.add(FriendshipKey.of(members.get(i), members.get(j)));
            }
        }
        return friendshipJdbcRepository.findAllForUpdate(keys);
    }

    @Override
    public void saveInterestScores(Collection<Friendship> friendships) {
        friendshipJdbcRepository.saveInterestScores(friendships);
    }

    @Override
    public void applyDecay(double rate, double threshold, LocalDateTime decayTime) {
        friendshipJdbcRepository.applyDecay(rate, threshold, decayTime);
    }
}
