package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkGraphResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialNetworkQueryService implements SocialNetworkQueryUseCase {

    private static final int PRUNING_EDGE_MIN   = 5;
    private static final int PRUNING_EDGE_RANGE = 10;
    private static final int STRANGER_QUOTA     = 5;

    private final SocialNetworkRepository socialNetworkRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    public NetworkGraphResult getFriendsNetwork(Long userId, DunbarCircle circleSize) {
        return socialNetworkRepository.getDefaultNetworkGraph(userId, circleSize, PRUNING_EDGE_MIN, PRUNING_EDGE_RANGE);
    }

    @Override
    public NetworkGraphResult getLabelNetwork(Long userId, String labelId) {
        return socialNetworkRepository.getLabelCustomNetwork(userId, labelId, DunbarCircle.DUNBAR, PRUNING_EDGE_MIN, PRUNING_EDGE_RANGE);
    }

    @Neo4jTransactional(readOnly = true)
    @Override
    public List<MutualFriendEdgeResult> getNewNodeEdges(Long userId, Long targetId, List<Long> skeletonIds) {
        if (skeletonIds == null || skeletonIds.isEmpty()) return List.of();
        double intimacy = friendshipRepository
                .findById(Friendship.generateCompositeId(userId, targetId))
                .map(Friendship::getIntimacy)
                .orElse(0.0);
        int dynamicLimit = (int) (5 + intimacy * 5);
        return socialNetworkRepository.getNewNodeEdges(userId, targetId, skeletonIds, dynamicLimit);
    }

    @Neo4jTransactional(readOnly = true)
    @Override
    public List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(Long userId, Long targetId, List<Long> skeletonIds) {
        if (skeletonIds == null || skeletonIds.isEmpty()) return List.of();
        return socialNetworkRepository.getNetworkContactsOfTwoHop(userId, targetId, skeletonIds, STRANGER_QUOTA);
    }
}
