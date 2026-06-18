package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

public record FriendshipDetailResult(
        Long friendId,
        String friendNickname,
        String friendProfileImageUrl,
        String friendAlias,
        double intimacy,
        double myInterestScore,
        boolean isMuted,
        boolean isRoutable
) {
    public static FriendshipDetailResult from(Friendship friendship, Long myId, ImageUrlResolverPort resolver) {
        UserReference friend = friendship.getFriend(myId);

        return new FriendshipDetailResult(
                friend.getId(),
                friend.getNickname(),
                resolver.resolveUrl(friend.getProfileImageUrl()),
                friendship.getFriendAlias(myId),
                friendship.getIntimacy(),
                friendship.getMyNormalizedInterestScore(myId),
                friendship.isMuted(myId),
                friendship.isRoutable(myId)
        );
    }
}