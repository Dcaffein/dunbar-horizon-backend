package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryAdapter implements FriendshipRepository {

    private static final int MYSQL_DUPLICATE_KEY = 1062;

    private final FriendshipJpaRepository friendships;
    private final EntityManager entityManager;

    @Override
    @Transactional
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
        return friendships.findBetween(Math.min(userId, friendId), Math.max(userId, friendId));
    }

    @Override
    public List<Friendship> findByUserId(Long userId) {
        return friendships.findByUserId(userId);
    }

    @Override
    public boolean existsFriendshipBetween(Long userId, Long friendId) {
        return friendships.countBetween(Math.min(userId, friendId), Math.max(userId, friendId)) > 0;
    }

    @Override
    public boolean deleteBetween(Long userId, Long friendId) {
        return friendships.deleteBetween(Math.min(userId, friendId), Math.max(userId, friendId)) > 0;
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
