package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.application.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.application.dto.FriendRequestView;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.application.port.out.FriendRequestQueryRepository;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipBroker friendshipBroker;
    private final FriendRequestQueryRepository friendRequestQueryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public FriendRequestResult sendRequest(Long currentUserId, Long counterpartId) {
        boolean existingFriendship = friendshipRepository.existsBetween(currentUserId, counterpartId);
        boolean existingRequest = friendRequestRepository.existsBetween(currentUserId, counterpartId);
        FriendRequest request = friendshipBroker.propose(currentUserId, counterpartId, existingFriendship, existingRequest);
        friendRequestRepository.create(request);
        FriendRequestView sent = friendRequestQueryRepository.findSentTo(request.getRequesterId(), request.getReceiverId())
                .orElseThrow(() -> new IllegalStateException("Created friend request could not be read"));
        return FriendRequestResult.fromSentRequest(request, sent);
    }

    public void cancelRequest(Long currentUserId, Long counterpartId) {
        FriendRequest request = friendRequestRepository.findBetween(currentUserId, counterpartId)
                .orElseThrow(() -> new FriendRequestNotFoundException(currentUserId, counterpartId));
        request.cancel(currentUserId);
        friendRequestRepository.delete(request);
    }

    public void updateStatus(Long currentUserId, Long counterpartId, FriendRequestStatus status) {
        FriendRequest request = friendRequestRepository.findBetween(currentUserId, counterpartId)
                .orElseThrow(() -> new FriendRequestNotFoundException(currentUserId, counterpartId));
        request.updateStatus(currentUserId, status);
        if (!request.isAccepted()) {
            friendRequestRepository.saveStatus(request);
            return;
        }

        friendRequestRepository.delete(request);
        boolean existingFriendship = friendshipRepository.existsBetween(request.getRequesterId(), request.getReceiverId());
        friendshipRepository.create(friendshipBroker.createFrom(request, existingFriendship));
        applicationEventPublisher.publishEvent(
                new FriendRequestAcceptedEvent(request.getRequesterId(), request.getReceiverId()));
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResult> getRequests(Long currentUserId, FriendRequestDirection direction,
                                                 FriendRequestStatus status) {
        if (direction == FriendRequestDirection.SENT) {
            if (status != null) {
                throw new FriendRequestInvalidException(SocialErrorCode.FRIEND_REQUEST_STATUS_NOT_ALLOWED_FOR_SENT,
                        ErrorContext.of("status", status));
            }
            return toResults(currentUserId, friendRequestQueryRepository.findSent(currentUserId));
        }
        FriendRequestStatus receivedStatus = status == null ? FriendRequestStatus.PENDING : status;
        return toResults(currentUserId, friendRequestQueryRepository.findReceived(currentUserId, receivedStatus));
    }

    private static List<FriendRequestResult> toResults(Long viewerId, List<FriendRequestView> found) {
        return found.stream().map(request -> FriendRequestResult.from(request, viewerId)).toList();
    }
}
