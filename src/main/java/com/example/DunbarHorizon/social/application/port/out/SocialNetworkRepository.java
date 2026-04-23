package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;

import java.util.List;

public interface SocialNetworkRepository {
    List<NetworkFriendEdgeResult> getDefaultIntimacyNetwork(Long userId, int limitSize);
    List<NetworkFriendEdgeResult> getLabelCustomNetwork(Long userId, String labelName, int limitSize);
    List<MutualFriendEdgeResult> getIntersectionByOneHop( Long userId, Long targetId, String labelName, int limitSize);
    List<NetworkOneHopsByTwoHopResult> getIntersectionOneHops(Long userId, Long targetId, String labelName, int limitSize);
}
