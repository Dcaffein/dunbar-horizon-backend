package com.example.GooRoomBe.social.domain.friend;

import com.example.GooRoomBe.social.domain.socialUser.UserReference;
import com.example.GooRoomBe.social.domain.friend.exception.*;
import com.example.GooRoomBe.social.domain.friend.repository.FriendRequestRepository;
import com.example.GooRoomBe.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendshipBroker {
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;

    public FriendRequest propose(UserReference requester, UserReference receiver) {
        Long requesterId = requester.getId();
        Long receiverId = receiver.getId();

        if(requester.getId().equals(receiver.getId())) {
            throw new CannotRequestToSelfException(requesterId);
        }

        if (friendshipRepository.existsFriendshipBetween(requesterId, receiverId)) {
            throw new AlreadyFriendsException(requesterId, receiverId);
        }

        if (friendRequestRepository.existsRequestBetween(requesterId, receiverId)) {
            throw new DuplicateFriendRequestException(requesterId, receiverId);
        }

        return new FriendRequest(requester, receiver);
    }

    public Friendship establish(FriendRequest friendRequest) {
        if (!friendRequest.isAccepted()) {
            throw new FriendRequestNotAcceptedException(friendRequest.getId());
        }

        if(friendRequest.getRequester()==null || friendRequest.getReceiver()==null){
            throw new FriendRequestInvalidException(friendRequest.getId());
        }

        if (friendshipRepository.existsFriendshipBetween(
                friendRequest.getRequester().getId(),
                friendRequest.getReceiver().getId())) {
            throw new AlreadyFriendsException(friendRequest.getRequester().getId(), friendRequest.getReceiver().getId());
        }

        return new Friendship(
                friendRequest.getRequester(),
                friendRequest.getReceiver()
        );
    }
}