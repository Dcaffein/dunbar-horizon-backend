package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.application.port.in.FriendRequestReceiverActionUseCase;
import com.example.DunbarHorizon.social.application.port.in.FriendRequesterActionUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialUserProfileReader;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class MySqlFriendRequestActionService implements FriendRequesterActionUseCase, FriendRequestReceiverActionUseCase {

    private final FriendRequestRepository requests;
    private final FriendshipRepository friendships;
    private final SocialUserProfileReader userProfiles;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
    public FriendRequestResult sendRequest(Long requesterId, Long receiverId) {
        Map<Long, SocialUserProfile> profiles = userProfiles.findActiveProfiles(List.of(requesterId, receiverId));
        SocialUserProfile requester = requireActiveProfile(profiles, requesterId);
        SocialUserProfile receiver = requireActiveProfile(profiles, receiverId);
        boolean existingFriendship = friendships.existsFriendshipBetween(requesterId, receiverId);
        boolean existingRequest = !existingFriendship && requests.existsBetween(requesterId, receiverId);
        FriendRequest request = FriendshipBroker.propose(requesterId, receiverId, existingFriendship, existingRequest);
        requests.create(request);
        if (friendships.existsFriendshipBetween(requesterId, receiverId)) {
            throw new AlreadyFriendsException(requesterId, receiverId);
        }
        return FriendRequestResult.from(request, requesterId, requester, receiver);
    }

    @Override
    public void cancelRequest(Long counterpartId, Long requesterId) {
        FriendRequest request = find(requesterId, counterpartId);
        request.cancel(requesterId);
        requests.delete(request);
    }

    @Override
    public void updateStatus(Long receiverId, Long counterpartId, FriendRequestStatus status) {
        FriendRequest request = find(receiverId, counterpartId);
        request.updateStatus(receiverId, status);
        if (!request.isAccepted()) {
            return;
        }
        requests.delete(request);
        boolean existingFriendship = friendships.existsFriendshipBetween(request.getRequesterId(), request.getReceiverId());
        Friendship friendship = FriendshipBroker.createFrom(request, existingFriendship);
        friendships.create(friendship);
        var receiver = requireActiveProfile(userProfiles.findActiveProfiles(List.of(receiverId)), receiverId);
        events.publishEvent(new FriendRequestAcceptedEvent(request.getRequesterId(), receiverId, receiver.nickname()));
    }

    private FriendRequest find(Long userId, Long counterpartId) {
        return requests.findBetween(userId, counterpartId)
                .orElseThrow(() -> new FriendRequestNotFoundException(userId, counterpartId));
    }

    private static SocialUserProfile requireActiveProfile(Map<Long, SocialUserProfile> profiles, Long id) {
        SocialUserProfile profile = profiles.get(id);
        if (profile == null) {
            throw new UserReferenceNotFoundException(id);
        }
        return profile;
    }
}
