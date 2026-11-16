package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.*;

public class FriendshipBroker {
    private FriendshipBroker() {}

    public static FriendRequest propose(Long requesterId, Long receiverId,
                                        boolean existingFriendship, boolean existingRequest) {
        if (existingFriendship) {
            throw new AlreadyFriendsException(requesterId, receiverId);
        }

        if (existingRequest) {
            throw new DuplicateFriendRequestException(requesterId, receiverId);
        }

        return new FriendRequest(requesterId, receiverId);
    }

    public static Friendship createFrom(FriendRequest friendRequest, boolean existingFriendship) {
        if (!friendRequest.isAccepted()) {
            throw new FriendRequestNotAcceptedException(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        }

        if (existingFriendship) {
            throw new AlreadyFriendsException(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        }

        return new Friendship(
                friendRequest.getRequesterId(),
                friendRequest.getReceiverId()
        );
    }
}
