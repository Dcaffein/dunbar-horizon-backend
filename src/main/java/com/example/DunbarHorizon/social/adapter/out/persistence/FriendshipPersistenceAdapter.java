package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.FriendshipJpaRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipIdRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipLockRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipScoreRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
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

    private static final int MYSQL_DUPLICATE_KEY = 1062;

    private final FriendshipJpaRepository jpa;
    private final FriendshipJdbcRepository jdbc;
    private final EntityManager entityManager;

    @Override
    public void create(Friendship friendship) {
        try {
            entityManager.persist(friendship);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (e instanceof EntityExistsException || isDuplicateKey(e)) {
                throw new AlreadyFriendsException(friendship.getLower().getOwnerId(), friendship.getHigher().getOwnerId());
            }
            throw e;
        }
    }

    @Override
    public Optional<Friendship> findBetween(Long userId, Long friendId) {
        var key = FriendshipKey.of(userId, friendId);
        return jpa.findBetween(key.low(), key.high());
    }

    @Override
    public List<Friendship> findByUserId(Long userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public boolean existsFriendshipBetween(Long userId, Long friendId) {
        var key = FriendshipKey.of(userId, friendId);
        return jpa.countBetween(key.low(), key.high()) > 0;
    }

    @Override
    public boolean deleteBetween(Long userId, Long friendId) {
        var key = FriendshipKey.of(userId, friendId);
        return jpa.deleteBetween(key.low(), key.high()) > 0;
    }

    @Override
    public Optional<Friendship> findBetweenForUpdate(Long userId, Long friendId) {
        return jdbc.findForUpdate(FriendshipKey.of(userId, friendId));
    }

    @Override
    public boolean existsFriendshipForUpdate(Long userId, Long friendId) {
        return jdbc.existsForUpdate(FriendshipKey.of(userId, friendId));
    }

    @Override
    public List<Friendship> findAllBetweenForUpdate(Long userId, Collection<Long> friendIds) {
        return jdbc.findAllForUpdate(pairs(friendIds.stream().map(friendId -> FriendshipKey.of(userId, friendId)).toList()));
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
        return jdbc.findAllForUpdate(keys);
    }

    @Override
    public void saveInterestScores(Collection<Friendship> friendships) {
        jdbc.saveInterestScores(friendships);
    }

    @Override
    public void applyDecay(double rate, double threshold, LocalDateTime decayTime) {
        jdbc.applyDecay(rate, threshold, decayTime);
    }

    @Override
    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted) {
        return jdbc.findFriendIdsByMuteStatus(userId, isMuted);
    }

    @Override
    public Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds) {
        return jdbc.filterFriendIdsAmong(userId, candidateIds);
    }

    private static List<FriendshipKey> pairs(Collection<FriendshipKey> keys) {
        return keys.stream().filter(FriendshipKey::isPair).distinct().toList();
    }

    private static boolean isDuplicateKey(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getErrorCode() == MYSQL_DUPLICATE_KEY) {
                return true;
            }
        }
        return false;
    }
}
