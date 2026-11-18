package com.example.DunbarHorizon.social.application.network;

import org.springframework.transaction.annotation.Transactional;
import com.example.DunbarHorizon.social.application.network.dto.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.network.dto.NodeGraphResult;
import com.example.DunbarHorizon.social.application.network.port.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.social.domain.friend.SocialNetworkExposurePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialNetworkQueryService {

    private static final int PRUNING_EDGE_MIN   = 5;
    private static final int PRUNING_EDGE_RANGE = 10;

    private final SocialNetworkRepository socialNetworkRepository;
    private final FriendshipRepository friendshipRepository;
    private final SocialNetworkExposurePolicy exposurePolicy;

    public List<NodeGraphResult> getFriendsNetwork(Long userId, DunbarCircle circleSize) {
        return socialNetworkRepository.getDefaultNetworkGraph(userId, circleSize, PRUNING_EDGE_MIN, PRUNING_EDGE_RANGE);
    }

    public List<NodeGraphResult> getLabelNetwork(Long userId, String labelId) {
        return socialNetworkRepository.getLabelCustomNetwork(userId, labelId, DunbarCircle.KINSHIP,PRUNING_EDGE_MIN, PRUNING_EDGE_RANGE);
    }

    @Transactional(readOnly = true)
    public List<MutualFriendEdgeResult> getNetworkEdges(Long userId, Long targetId, List<Long> baseNetworkFriendIds) {
        if (baseNetworkFriendIds == null || baseNetworkFriendIds.isEmpty()) return List.of();
        return friendshipRepository.findBetween(userId, targetId)
                .map(friendship -> socialNetworkRepository.getDirectFriendEdgesForTarget(
                        userId, targetId, baseNetworkFriendIds,
                        exposurePolicy.directFriendEdgeLimit(friendship.getIntimacy())))
                .orElseGet(() -> socialNetworkRepository.getTwoHopContactEdgesForTarget(
                        userId, targetId, baseNetworkFriendIds, exposurePolicy.twoHopContactEdgeLimit()));
    }
}
