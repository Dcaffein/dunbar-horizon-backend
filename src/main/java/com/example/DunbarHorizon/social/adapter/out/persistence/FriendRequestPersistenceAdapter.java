package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.FriendRequestJpaRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
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
    private final EntityManager entityManager;

    @Override
    public void create(FriendRequest request) {
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
        return friendRequestJpaRepository.findBetween(userId, counterpartId);
    }

    @Override
    public boolean existsBetween(Long userId, Long counterpartId) {
        return friendRequestJpaRepository.countBetween(userId, counterpartId) > 0;
    }

    @Override
    public void delete(FriendRequest request) {
        entityManager.remove(request);
    }

    @Override
    public List<FriendRequest> findAllByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status) {
        return friendRequestJpaRepository.findAllByReceiverIdAndStatus(receiverId, status.name());
    }

    @Override
    public List<FriendRequest> findAllByRequesterId(Long requesterId) {
        return friendRequestJpaRepository.findAllByRequesterId(requesterId);
    }
}
