package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.dto.FriendView;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;

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
    public static FriendshipDetailResult from(FriendView friend, ImageUrlResolverPort resolver) {
        return new FriendshipDetailResult(
                friend.friendId(),
                friend.nickname(),
                resolver.resolveUrl(friend.imageKey()),
                friend.friendAlias(),
                friend.intimacy(),
                friend.myNormalizedInterestScore(),
                friend.muted(),
                friend.routable()
        );
    }
}
