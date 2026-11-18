package com.example.DunbarHorizon.social.application.network.port;

import com.example.DunbarHorizon.social.application.network.dto.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.network.dto.NodeGraphResult;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;

import java.util.List;

public interface SocialNetworkRepository {
    List<NodeGraphResult> getDefaultNetworkGraph(Long userId, DunbarCircle circleSize, int pruningMin, int pruningRange);
    List<NodeGraphResult> getLabelCustomNetwork(Long userId, String labelId, DunbarCircle circleSize, int pruningMin, int pruningRange);

    List<MutualFriendEdgeResult> getDirectFriendEdgesForTarget(Long userId, Long targetId, List<Long> baseNetworkFriendIds, int dynamicLimit);
    List<MutualFriendEdgeResult> getTwoHopContactEdgesForTarget(Long userId, Long targetId, List<Long> baseNetworkFriendIds, int strangerLimit);
}
