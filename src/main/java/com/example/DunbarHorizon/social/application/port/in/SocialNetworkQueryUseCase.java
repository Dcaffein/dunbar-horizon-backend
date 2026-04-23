package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;

import java.util.List;

public interface SocialNetworkQueryUseCase {
    List<NetworkFriendEdgeResult> getFriendsNetwork(Long userId, int limitSize);
    List<NetworkFriendEdgeResult> getLabelNetwork(Long userId, String labelName, int limitSize);
    List<MutualFriendEdgeResult> getIntersectionByOneHop(Long userId, Long targetId, String labelName, int limitSize);
    List<NetworkOneHopsByTwoHopResult> getIntersectionByTwoHop(Long userId, Long targetId, String labelName, int limitSize);
}
