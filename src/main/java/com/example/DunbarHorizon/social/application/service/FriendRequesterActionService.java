package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.port.in.FriendRequesterActionUseCase;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendRequesterActionService implements FriendRequesterActionUseCase {
    private final FriendRequestRepository friendRequestRepository;
    private final SocialUserRepository socialUserRepository;
    private final FriendshipBroker friendshipBroker;

    @Override
    public FriendRequest sendRequest(Long requesterId, Long receiverId) {
        UserReference requester = socialUserRepository.findById(requesterId)
                .orElseThrow(() -> new UserReferenceNotFoundException(requesterId));
        UserReference receiver = socialUserRepository.findById(receiverId)
                .orElseThrow(() -> new UserReferenceNotFoundException(receiverId));

        FriendRequest newRequest = friendshipBroker.propose(requester, receiver);

        return friendRequestRepository.saveRequest(newRequest);
    }

    @Override
    public void cancelRequest(String requestId, Long requesterId) {
        FriendRequest request = findRequestById(requestId);
        request.validateCancelBy(requesterId);
        friendRequestRepository.deleteById(requestId);
    }


    private FriendRequest findRequestById(String requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));
    }
}
