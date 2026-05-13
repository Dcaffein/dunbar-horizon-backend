package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheRepository;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import lombok.RequiredArgsConstructor;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class SocialNetworkQueryService implements SocialNetworkQueryUseCase {

    private final SocialNetworkRepository socialNetworkRepository;
    private final SocialNetworkCacheRepository cacheRepository;

    @Override
    public List<NetworkFriendEdgeResult> getFriendsNetwork(Long userId, DunbarCircle circleSize) {
        return cacheRepository.getDefaultNetwork(userId, circleSize)
                .orElseGet(() -> {
                    List<NetworkFriendEdgeResult> result =
                            socialNetworkRepository.getDefaultIntimacyNetwork(userId, circleSize.getLimitSize());
                    cacheRepository.putDefaultNetwork(userId, circleSize, result);
                    return result;
                });
    }

    @Override
    public List<NetworkFriendEdgeResult> getLabelNetwork(Long userId, String labelId) {
        return cacheRepository.getLabelNetwork(userId, labelId)
                .orElseGet(() -> {
                    List<NetworkFriendEdgeResult> result =
                            socialNetworkRepository.getLabelCustomNetwork(userId, labelId, DunbarCircle.DUNBAR.getLimitSize());
                    cacheRepository.putLabelNetwork(userId, labelId, result);
                    return result;
                });
    }

    @Override
    public List<MutualFriendEdgeResult> getIntersectionByOneHop(
            Long userId, Long targetId, String labelName, int limitSize) {
        return socialNetworkRepository.getIntersectionByOneHop(userId, targetId, labelName, limitSize);
    }

    @Override
    public List<NetworkOneHopsByTwoHopResult> getIntersectionByTwoHop(
            Long userId, Long targetId, String labelName, int limitSize) {
        return socialNetworkRepository.getIntersectionOneHops(userId, targetId, labelName, limitSize);
    }
}
