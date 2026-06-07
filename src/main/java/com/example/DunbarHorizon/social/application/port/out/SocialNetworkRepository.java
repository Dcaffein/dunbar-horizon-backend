package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;

import java.util.List;

public interface SocialNetworkRepository {
    List<NetworkFriendEdgeResult> getDefaultIntimacyNetwork(Long userId, DunbarCircle circleSize);
    List<NetworkFriendEdgeResult> getLabelCustomNetwork(Long userId, String labelId);

    List<MutualFriendEdgeResult> getNewNodeEdges(Long userId, Long targetId, String labelId, int limitSize);
    List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(Long userId, Long targetId, String labelId, int limitSize);
}
