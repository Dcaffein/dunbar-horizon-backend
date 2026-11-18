package com.example.DunbarHorizon.social.domain.friendship;


public class FriendTestFactory {

    public static FriendRequest createRequest(Long req, Long res) {
        return new FriendRequest(req, res);
    }

    public static FriendRequest createAcceptedRequest(Long req, Long res) {
        FriendRequest request = new FriendRequest(req, res);
        request.updateStatus(res, FriendRequestStatus.ACCEPTED);
        return request;
    }

    public static Friendship createFriendship(Long userA, Long userB) {
        return new Friendship(userA, userB);
    }

    public static Friendship createFriendshipWithIntimacy(Long userA, Long userB, double intimacyDelta) {
        Friendship friendship = new Friendship(userA, userB);
        friendship.adjustInterestScore(userA, intimacyDelta);
        return friendship;
    }
}
