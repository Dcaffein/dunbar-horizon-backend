package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ActiveUserJpaRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.FriendRequestJpaRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestId;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendRequestPersistenceAdapter implements FriendRequestRepository {

    private final FriendRequestJpaRepository friendRequestJpaRepository;
    private final ActiveUserJpaRepository activeUserJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void create(FriendRequest request) {
        for (Long userId : List.of(request.getRequesterId(), request.getReceiverId())) {
            if (!activeUserJpaRepository.existsById(userId)) {
                throw new UserReferenceNotFoundException(userId);
            }
        }
        try {
            entityManager.persist(request);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (e instanceof EntityExistsException || PersistenceErrors.isDuplicateKey(e)) {
                throw new DuplicateFriendRequestException(request.getRequesterId(), request.getReceiverId());
            }
            throw e;
        }
    }

    @Override
    public Optional<FriendRequest> findBetween(Long userId, Long counterpartId) {
        return friendRequestJpaRepository.findById(new FriendRequestId(userId, counterpartId))
                .or(() -> friendRequestJpaRepository.findById(new FriendRequestId(counterpartId, userId)));
    }

    @Override
    public boolean existsBetween(Long userId, Long counterpartId) {
        return friendRequestJpaRepository.existsById(new FriendRequestId(userId, counterpartId))
                || friendRequestJpaRepository.existsById(new FriendRequestId(counterpartId, userId));
    }

    @Override
    public void delete(FriendRequest request) {
        entityManager.remove(request);
    }

    @Override
    public List<FriendRequest> findAllByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status) {
        return friendRequestJpaRepository.findAllByReceiverIdAndStatus(receiverId, status);
    }

    @Override
    public List<FriendRequest> findAllByRequesterId(Long requesterId) {
        return friendRequestJpaRepository.findAllByRequesterId(requesterId);
    }
}
