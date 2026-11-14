package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;
import com.example.DunbarHorizon.social.application.port.in.FriendRequestReceiverActionUseCase;
import com.example.DunbarHorizon.social.application.port.in.FriendRequesterActionUseCase;
import com.example.DunbarHorizon.social.application.port.out.FriendRequestCommandRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipCreationRepository;
import com.example.DunbarHorizon.social.application.port.out.SocialUserProfileReader;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class MySqlFriendRequestActionService implements FriendRequesterActionUseCase, FriendRequestReceiverActionUseCase {

    private final FriendRequestCommandRepository requests;
    private final FriendshipCreationRepository friendships;
    private final SocialUserProfileReader userProfiles;
    private final ApplicationEventPublisher events;

    @Override
    public FriendRequest sendRequest(Long requesterId, Long receiverId) {
        Map<Long, SocialUserProfile> profiles = userProfiles.findActiveProfiles(List.of(requesterId, receiverId));
        SocialUserProfile requester = requireActiveProfile(profiles, requesterId);
        SocialUserProfile receiver = requireActiveProfile(profiles, receiverId);
        FriendRequest request = FriendshipBroker.propose(requester, receiver, friendships::existsFriendshipBetween,
                requests::existsRequestBetween);
        requests.saveRequest(request);
        // A concurrent acceptance can delete the old request while this insert waits.
        if (friendships.existsFriendshipForUpdate(requesterId, receiverId)) {
            throw new AlreadyFriendsException(requesterId, receiverId);
        }
        return request;
    }

    @Override
    public void cancelRequest(Long counterpartId, Long requesterId) {
        FriendRequest request = findForUpdate(requesterId, counterpartId);
        request.cancel(requesterId);
        requests.deleteById(request.getId());
    }

    @Override
    public void updateStatus(Long receiverId, Long counterpartId, FriendRequestStatus status) {
        FriendRequest request = findForUpdate(receiverId, counterpartId);
        request.updateStatus(receiverId, status);
        if (request.isAccepted()) {
            Friendship friendship = FriendshipBroker.createFrom(request, friendships::existsFriendshipForUpdate);
            friendships.create(friendship);
            requests.deleteById(request.getId());
            events.publishEvent(new FriendRequestAcceptedEvent(request.getRequester().getId(),
                    request.getReceiver().getId(), request.getReceiver().getNickname()));
        } else {
            requests.updateStatus(request);
        }
    }

    private FriendRequest findForUpdate(Long userId, Long counterpartId) {
        String id = FriendRequest.generateId(userId, counterpartId);
        return requests.findByIdForUpdate(id).orElseThrow(() -> new FriendRequestNotFoundException(id));
    }

    private static SocialUserProfile requireActiveProfile(Map<Long, SocialUserProfile> profiles, Long id) {
        SocialUserProfile profile = profiles.get(id);
        if (profile == null) {
            throw new UserReferenceNotFoundException(id);
        }
        return profile;
    }
}
