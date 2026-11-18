package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.application.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.application.dto.FriendRequestView;
import com.example.DunbarHorizon.social.application.dto.UserProfile;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.application.port.out.FriendRequestViewRepository;
import com.example.DunbarHorizon.social.application.port.out.UserProfileRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestViewRepository friendRequestViewRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public FriendRequestResult sendRequest(Long currentUserId, Long counterpartId) {
        boolean existingFriendship = friendshipRepository.existsFriendshipBetween(currentUserId, counterpartId);
        boolean existingRequest = friendRequestRepository.existsBetween(currentUserId, counterpartId);
        FriendRequest request = FriendshipBroker.propose(currentUserId, counterpartId, existingFriendship, existingRequest);
        friendRequestRepository.create(request);
        Map<Long, UserProfile> profiles = userProfileRepository.findActiveProfiles(List.of(currentUserId, counterpartId));
        return FriendRequestResult.from(request, currentUserId, profiles.get(currentUserId), profiles.get(counterpartId));
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
        boolean existingFriendship = friendshipRepository.existsFriendshipBetween(request.getRequesterId(), request.getReceiverId());
        friendshipRepository.create(FriendshipBroker.createFrom(request, existingFriendship));
        UserProfile receiver = userProfileRepository.findActiveProfiles(List.of(request.getReceiverId()))
                .get(request.getReceiverId());
        applicationEventPublisher.publishEvent(
                new FriendRequestAcceptedEvent(request.getRequesterId(), request.getReceiverId(), receiver.nickname()));
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResult> getRequests(Long currentUserId, FriendRequestDirection direction,
                                                 FriendRequestStatus status) {
        if (direction == FriendRequestDirection.SENT) {
            if (status != null) {
                throw new FriendRequestInvalidException(SocialErrorCode.FRIEND_REQUEST_STATUS_NOT_ALLOWED_FOR_SENT,
                        ErrorContext.of("status", status));
            }
            return toResults(currentUserId, friendRequestViewRepository.findSent(currentUserId));
        }
        FriendRequestStatus receivedStatus = status == null ? FriendRequestStatus.PENDING : status;
        return toResults(currentUserId, friendRequestViewRepository.findReceived(currentUserId, receivedStatus));
    }

    private static List<FriendRequestResult> toResults(Long viewerId, List<FriendRequestView> found) {
        return found.stream().map(request -> FriendRequestResult.from(request, viewerId)).toList();
    }
}
