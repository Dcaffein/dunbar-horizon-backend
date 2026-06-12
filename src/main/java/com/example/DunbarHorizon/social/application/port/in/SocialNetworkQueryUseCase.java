package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkGraphResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;

import java.util.List;

public interface SocialNetworkQueryUseCase {
    NetworkGraphResult getFriendsNetwork(Long userId, DunbarCircle circleSize);
    NetworkGraphResult getLabelNetwork(Long userId, String labelId);
    List<MutualFriendEdgeResult> getNewNodeEdges(Long userId, Long targetId, List<Long> skeletonIds);
    List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(Long userId, Long targetId, List<Long> skeletonIds);
}
