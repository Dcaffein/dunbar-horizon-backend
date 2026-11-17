package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.application.dto.UserProfile;

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
    public static FriendshipDetailResult from(Friendship friendship, Long myId, UserProfile friend, ImageUrlResolverPort resolver) {
        return new FriendshipDetailResult(
                friend.id(),
                friend.nickname(),
                resolver.resolveUrl(friend.imageKey()),
                friendship.getFriendAlias(myId),
                friendship.getIntimacy(),
                friendship.getMyNormalizedInterestScore(myId),
                friendship.isMuted(myId),
                friendship.isRoutable(myId)
        );
    }
}
