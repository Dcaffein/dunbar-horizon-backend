package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
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

    @Override
    public List<NetworkFriendEdgeResult> getFriendsNetwork(Long userId, DunbarCircle circleSize) {
        return socialNetworkRepository.getDefaultIntimacyNetwork(userId, circleSize);
    }

    @Override
    public List<NetworkFriendEdgeResult> getLabelNetwork(Long userId, String labelId) {
        return socialNetworkRepository.getLabelCustomNetwork(userId, labelId);
    }

    @Override
    public List<MutualFriendEdgeResult> getNewNodeEdges(
            Long userId, Long targetId, String labelId, int limitSize) {
        return socialNetworkRepository.getNewNodeEdges(userId, targetId, labelId, limitSize);
    }

    @Override
    public List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(
            Long userId, Long targetId, String labelId, int limitSize) {
        return socialNetworkRepository.getNetworkContactsOfTwoHop(userId, targetId, labelId, limitSize);
    }
}
