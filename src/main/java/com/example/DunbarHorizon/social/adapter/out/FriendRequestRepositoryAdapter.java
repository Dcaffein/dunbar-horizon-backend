package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.neo4j.springData.FriendRequestNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FriendRequestRepositoryAdapter implements FriendRequestRepository {

    private final FriendRequestNeo4jRepository friendRequestNeo4jRepository;

    @Override
    public FriendRequest saveRequest(FriendRequest request) {
        if (!friendRequestNeo4jRepository.existsById(request.getId())) {
            return friendRequestNeo4jRepository.mergeFriendRequest(
                    request.getRequester().getId(),
                    request.getReceiver().getId(),
                    request.getId()
            );
        }
        return friendRequestNeo4jRepository.updateFriendRequest(request);
    }

    @Override
    public boolean existsRequestBetween(Long requesterId, Long receiverId) {
        return friendRequestNeo4jRepository.existsRequestBetween(requesterId, receiverId);
    }

    @Override
    public Optional<FriendRequest> findById(String requestId) {
        return friendRequestNeo4jRepository.findById(requestId);
    }

    @Override
    public void deleteById(String requestId) {
        friendRequestNeo4jRepository.deleteById(requestId);
    }

    @Override
    public List<FriendRequest> findAllByReceiver_IdAndStatus(Long receiverId, FriendRequestStatus status) {
        return friendRequestNeo4jRepository.findAllByReceiver_IdAndStatus(receiverId, status);
    }

    @Override
    public List<FriendRequest> findAllByRequester_IdAndStatus(Long requesterId, FriendRequestStatus status) {
        return friendRequestNeo4jRepository.findAllByRequester_IdAndStatus(requesterId, status);
    }

    @Override
    public void deleteOldHiddenRequests(LocalDateTime threshold) {
        friendRequestNeo4jRepository.deleteOldHiddenRequests(threshold);
    }
}
