package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import org.springframework.stereotype.Component;

@Component
public class FriendshipBroker {

    public FriendRequest propose(Long requesterId, Long receiverId,
                                 boolean existingFriendship, boolean existingRequest) {
        if (existingFriendship) {
            throw new AlreadyFriendsException(requesterId, receiverId);
        }

        if (existingRequest) {
            throw new DuplicateFriendRequestException(requesterId, receiverId);
        }

        return new FriendRequest(requesterId, receiverId);
    }

    public Friendship createFrom(FriendRequest friendRequest, boolean existingFriendship) {
        if (existingFriendship) {
            throw new AlreadyFriendsException(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        }

        return friendRequest.toFriendship();
    }
}
