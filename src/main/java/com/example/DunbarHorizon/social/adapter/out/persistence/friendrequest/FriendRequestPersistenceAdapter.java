package com.example.DunbarHorizon.social.adapter.out.persistence.friendrequest;

import com.example.DunbarHorizon.social.adapter.out.persistence.support.PersistenceErrors;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequest;
import com.example.DunbarHorizon.social.domain.friendship.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendRequestConflictException;
import com.example.DunbarHorizon.social.domain.friendship.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.exception.UserReferenceNotFoundException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendRequestPersistenceAdapter implements FriendRequestRepository {

    private static final String SAME_DIRECTION_KEY = "PRIMARY";
    private static final String EITHER_DIRECTION_KEY = "uk_friend_requests_pair";

    private final FriendRequestJpaRepository friendRequestJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void create(FriendRequest request) {
        friendRequestJpaRepository.findUnavailableParticipant(request.getRequesterId(), request.getReceiverId())
                .ifPresent(userId -> {
                    throw new UserReferenceNotFoundException(userId);
                });
        try {
            entityManager.persist(request);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (e instanceof EntityExistsException
                    || PersistenceErrors.violates(e, SAME_DIRECTION_KEY)
                    || PersistenceErrors.violates(e, EITHER_DIRECTION_KEY)) {
                throw new DuplicateFriendRequestException(request.getRequesterId(), request.getReceiverId());
            }
            throw e;
        }
    }

    @Override
    public Optional<FriendRequest> findBetween(Long userId, Long counterpartId) {
        return friendRequestJpaRepository.findBetweenActiveUsers(userId, counterpartId);
    }

    @Override
    public boolean existsBetween(Long userId, Long counterpartId) {
        return friendRequestJpaRepository.existsBetweenActiveUsers(userId, counterpartId);
    }

    @Override
    public void saveStatus(FriendRequest request) {
        flushOrConflict(request);
    }

    @Override
    public void delete(FriendRequest request) {
        entityManager.remove(request);
        flushOrConflict(request);
    }

    private void flushOrConflict(FriendRequest request) {
        try {
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw new FriendRequestConflictException(request.getRequesterId(), request.getReceiverId());
        }
    }
}
