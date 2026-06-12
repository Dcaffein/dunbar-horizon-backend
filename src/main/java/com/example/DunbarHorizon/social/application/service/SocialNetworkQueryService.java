package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class SocialNetworkQueryService implements SocialNetworkQueryUseCase {

    private final SocialNetworkRepository socialNetworkRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    public List<NetworkFriendEdgeResult> getFriendsNetwork(Long userId, DunbarCircle circleSize) {
        return socialNetworkRepository.getDefaultIntimacyNetwork(userId, circleSize);
    }

    @Override
    public List<NetworkFriendEdgeResult> getLabelNetwork(Long userId, String labelId) {
        return socialNetworkRepository.getLabelCustomNetwork(userId, labelId);
    }

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

    @Override
    public List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(Long userId, Long targetId, List<Long> skeletonIds) {
        if (skeletonIds == null || skeletonIds.isEmpty()) return List.of();
        return socialNetworkRepository.getNetworkContactsOfTwoHop(userId, targetId, skeletonIds);
    }
}
