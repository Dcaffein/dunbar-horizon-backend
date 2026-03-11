package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialNetworkQueryService implements SocialNetworkQueryUseCase {

    private static final int DEFAULT_BOUNDARY_SIZE = 150;
    private static final int DEFAULT_CORE_SIZE = 50;

    private final SocialNetworkRepository socialNetworkRepository;

    @Override
    public List<NetworkFriendEdgeResult> getFriendsNetwork(Long userId) {
        return socialNetworkRepository.getFriendsNetwork(userId);
    }

    @Override
    public List<NetworkFriendEdgeResult> getVerifiedFriendsNetwork(Long userId, Collection<Long> targetIds) {
        return socialNetworkRepository.getVerifiedFriendsNetwork(userId, targetIds);
    }

    @Override
    public List<NetworkFriendEdgeResult> getTopIntimateFriendsNetwork(Long userId) {
        return socialNetworkRepository.getTopIntimateFriendsNetwork(userId, DEFAULT_BOUNDARY_SIZE, DEFAULT_CORE_SIZE);
    }

    @Override
    public List<NetworkFriendEdgeResult> getTopInterestFriendsNetwork(Long userId) {
        return socialNetworkRepository.getTopInterestFriendsNetwork(userId, DEFAULT_BOUNDARY_SIZE, DEFAULT_CORE_SIZE);
    }


    @Override
    public List<NetworkOneHopsByTwoHopResult> getIntersectionByTwoHop(Long userId, Long targetId) {
        return socialNetworkRepository.getIntersectionOneHops(userId, targetId);
    }

    @Override
    public List<MutualFriendEdgeResult> getIntersectionByOneHop(Long userId, Long targetId) {
        return socialNetworkRepository.getIntersectionByOneHop(userId, targetId);
    }
}
