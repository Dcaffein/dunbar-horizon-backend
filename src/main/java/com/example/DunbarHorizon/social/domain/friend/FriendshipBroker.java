package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.*;

import java.util.function.BiPredicate;

public class FriendshipBroker {
    private FriendshipBroker() {}

    public static FriendRequest propose(Long requesterId, Long receiverId,
                                        BiPredicate<Long, Long> areFriends, BiPredicate<Long, Long> requestExists) {
        if (areFriends.test(requesterId, receiverId)) {
            throw new AlreadyFriendsException(requesterId, receiverId);
        }

        if (requestExists.test(requesterId, receiverId) || requestExists.test(receiverId, requesterId)) {
            throw new DuplicateFriendRequestException(requesterId, receiverId);
        }

        return new FriendRequest(requesterId, receiverId);
    }

    public static Friendship createFrom(FriendRequest friendRequest, BiPredicate<Long, Long> areFriends) {
        if (!friendRequest.isAccepted()) {
            throw new FriendRequestNotAcceptedException(friendRequest.getId());
        }

        if (areFriends.test(
                friendRequest.getRequesterId(),
                friendRequest.getReceiverId())) {
            throw new AlreadyFriendsException(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        }

        return new Friendship(
                friendRequest.getRequesterId(),
                friendRequest.getReceiverId()
        );
    }
}
