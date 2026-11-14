package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.friend.exception.*;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.BiPredicate;

@Component
@RequiredArgsConstructor
public class FriendshipBroker {
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;

    public FriendRequest propose(UserReference requester, UserReference receiver) {
        return propose(requester, receiver, friendshipRepository::existsFriendshipBetween,
                friendRequestRepository::existsRequestBetween);
    }

    public static FriendRequest propose(UserReference requester, UserReference receiver,
                                        BiPredicate<Long, Long> areFriends, BiPredicate<Long, Long> requestExists) {
        Long requesterId = requester.getId();
        Long receiverId = receiver.getId();

        if (areFriends.test(requesterId, receiverId)) {
            throw new AlreadyFriendsException(requesterId, receiverId);
        }

        if (requestExists.test(requesterId, receiverId) || requestExists.test(receiverId, requesterId)) {
            throw new DuplicateFriendRequestException(requesterId, receiverId);
        }

        return new FriendRequest(requester, receiver);
    }

    public Friendship createFrom(FriendRequest friendRequest) {
        return createFrom(friendRequest, friendshipRepository::existsFriendshipBetween);
    }

    public static Friendship createFrom(FriendRequest friendRequest, BiPredicate<Long, Long> areFriends) {
        if (!friendRequest.isAccepted()) {
            throw new FriendRequestNotAcceptedException(friendRequest.getId());
        }

        if (areFriends.test(
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
