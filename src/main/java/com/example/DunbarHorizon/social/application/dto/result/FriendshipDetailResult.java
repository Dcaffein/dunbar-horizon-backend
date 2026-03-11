package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

public record FriendshipDetailResult(
        Long friendId,
        String friendNickname,
        String friendProfileImageUrl,
        String friendAlias,
        double intimacy,
        double myInterestScore,
        boolean isMuted
) {
    public static FriendshipDetailResult from(com.example.DunbarHorizon.social.domain.friend.Friendship friendship, Long myId) {
        UserReference friend = friendship.getFriend(myId);

        return new FriendshipDetailResult(
                friend.getId(),
                friend.getNickname(),
                friend.getProfileImageUrl(),
                friendship.getFriendAlias(myId),
                friendship.getIntimacy(),
                friendship.getMyNormalizedInterestScore(myId),
                friendship.isMuted(myId)
        );
    }
}