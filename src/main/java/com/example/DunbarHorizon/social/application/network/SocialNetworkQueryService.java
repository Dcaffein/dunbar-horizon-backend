package com.example.DunbarHorizon.social.application.network;

import org.springframework.transaction.annotation.Transactional;
import com.example.DunbarHorizon.social.application.network.dto.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.network.dto.NodeGraphResult;
import com.example.DunbarHorizon.social.application.network.port.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.network.DunbarCircle;
import com.example.DunbarHorizon.social.domain.network.NetworkEdgeLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialNetworkQueryService {

    private static final int PRUNING_EDGE_MIN   = 5;
    private static final int PRUNING_EDGE_RANGE = 10;

    private final SocialNetworkRepository socialNetworkRepository;
    private final NetworkEdgeLimits edgeLimits;

    public List<NodeGraphResult> getFriendsNetwork(Long userId, DunbarCircle circleSize) {
        return socialNetworkRepository.getDefaultNetworkGraph(userId, circleSize, PRUNING_EDGE_MIN, PRUNING_EDGE_RANGE);
    }

    public List<NodeGraphResult> getLabelNetwork(Long userId, String labelId) {
        return socialNetworkRepository.getLabelCustomNetwork(userId, labelId, DunbarCircle.KINSHIP,PRUNING_EDGE_MIN, PRUNING_EDGE_RANGE);
    }

    @Transactional(readOnly = true)
    public List<MutualFriendEdgeResult> getNetworkEdges(Long userId, Long targetId, List<Long> baseNetworkFriendIds) {
        if (baseNetworkFriendIds == null || baseNetworkFriendIds.isEmpty()) return List.of();
        return socialNetworkRepository.findIntimacy(userId, targetId)
                .map(intimacy -> socialNetworkRepository.getDirectFriendEdgesForTarget(
                        userId, targetId, baseNetworkFriendIds,
                        edgeLimits.directFriendEdgeLimit(intimacy)))
                .orElseGet(() -> socialNetworkRepository.getTwoHopContactEdgesForTarget(
                        userId, targetId, baseNetworkFriendIds, edgeLimits.twoHopContactEdgeLimit()));
    }
}
