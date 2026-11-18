package com.example.DunbarHorizon.social.adapter.out.persistence.friendship;

import com.example.DunbarHorizon.social.adapter.out.persistence.support.PersistenceErrors;
import com.example.DunbarHorizon.social.domain.friendship.Friendship;
import com.example.DunbarHorizon.social.domain.friendship.FriendshipId;
import com.example.DunbarHorizon.social.domain.friendship.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendshipConflictException;
import com.example.DunbarHorizon.social.domain.friendship.repository.FriendshipRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendshipPersistenceAdapter implements FriendshipRepository {

    private static final String PAIR_KEY = "PRIMARY";

    private final FriendshipJpaRepository friendshipJpaRepository;
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
        var id = FriendshipId.of(userId, friendId);
        return friendshipJpaRepository.findBetweenActiveUsers(id.getLowUserId(), id.getHighUserId());
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
        var id = FriendshipId.of(userId, friendId);
        return friendshipJpaRepository.existsBetweenActiveUsers(id.getLowUserId(), id.getHighUserId());
    }

    @Override
    public boolean deleteBetween(Long userId, Long friendId) {
        var id = FriendshipId.of(userId, friendId);
        return friendshipJpaRepository.deleteBetweenActiveUsers(id.getLowUserId(), id.getHighUserId()) > 0;
    }
}
