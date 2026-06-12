package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkGraphResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;

import java.util.List;

public interface SocialNetworkRepository {
    NetworkGraphResult getDefaultNetworkGraph(Long userId, DunbarCircle circleSize);
    NetworkGraphResult getLabelCustomNetwork(Long userId, String labelId);

    List<MutualFriendEdgeResult> getNewNodeEdges(Long userId, Long targetId, List<Long> skeletonIds, int dynamicLimit);
    List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(Long userId, Long targetId, List<Long> skeletonIds);
}
