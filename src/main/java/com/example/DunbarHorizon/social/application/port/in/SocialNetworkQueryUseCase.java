package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;

import java.util.Collection;
import java.util.List;

public interface SocialNetworkQueryUseCase {
    List<NetworkFriendEdgeResult> getFriendsNetwork(Long userId);
    List<NetworkFriendEdgeResult> getVerifiedFriendsNetwork(Long userId, Collection<Long> targetIds);
    List<NetworkFriendEdgeResult> getTopIntimateFriendsNetwork(Long userId);
    List<NetworkFriendEdgeResult> getTopInterestFriendsNetwork(Long userId);
    List<NetworkOneHopsByTwoHopResult> getIntersectionByTwoHop(Long userId, Long targetId);
    List<MutualFriendEdgeResult> getIntersectionByOneHop(Long userId, Long targetId);
}
