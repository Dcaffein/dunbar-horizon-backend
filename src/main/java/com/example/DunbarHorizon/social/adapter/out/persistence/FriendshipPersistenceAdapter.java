package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ActiveUserJpaRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.FriendshipJpaRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipIdRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipLockRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipScoreRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipId;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class FriendshipPersistenceAdapter
        implements FriendshipRepository, FriendshipScoreRepository, FriendshipLockRepository, FriendshipIdRepository {

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
        if (!activeUserJpaRepository.areActive(userId, friendId)) {
            return Optional.empty();
        }
        var key = FriendshipKey.of(userId, friendId);
        return friendshipJpaRepository.findById(new FriendshipId(key.low(), key.high()));
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
    public boolean existsFriendshipBetween(Long userId, Long friendId) {
        if (!activeUserJpaRepository.areActive(userId, friendId)) {
            return false;
        }
        var key = FriendshipKey.of(userId, friendId);
        return friendshipJpaRepository.existsById(new FriendshipId(key.low(), key.high()));
    }

    @Override
    public boolean deleteBetween(Long userId, Long friendId) {
        if (!activeUserJpaRepository.areActive(userId, friendId)) {
            return false;
        }
        var key = FriendshipKey.of(userId, friendId);
        return friendshipJpaRepository.deleteBetween(key.low(), key.high()) > 0;
    }

    @Override
    public Optional<Friendship> findBetweenForUpdate(Long userId, Long friendId) {
        return friendshipJdbcRepository.findForUpdate(FriendshipKey.of(userId, friendId));
    }

    @Override
    public boolean existsFriendshipForUpdate(Long userId, Long friendId) {
        return friendshipJdbcRepository.existsForUpdate(FriendshipKey.of(userId, friendId));
    }

    @Override
    public List<Friendship> findAllBetweenForUpdate(Long userId, Collection<Long> friendIds) {
        return friendshipJdbcRepository.findAllForUpdate(pairs(friendIds.stream().map(friendId -> FriendshipKey.of(userId, friendId)).toList()));
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

    @Override
    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted) {
        return friendshipJdbcRepository.findFriendIdsByMuteStatus(userId, isMuted);
    }

    @Override
    public Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds) {
        return friendshipJdbcRepository.filterFriendIdsAmong(userId, candidateIds);
    }

    private static List<FriendshipKey> pairs(Collection<FriendshipKey> keys) {
        return keys.stream().filter(FriendshipKey::isPair).distinct().toList();
    }
}
