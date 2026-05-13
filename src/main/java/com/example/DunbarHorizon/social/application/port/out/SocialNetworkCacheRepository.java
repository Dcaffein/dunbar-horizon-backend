package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;

import java.util.List;
import java.util.Optional;

public interface SocialNetworkCacheRepository {

    Optional<List<NetworkFriendEdgeResult>> getDefaultNetwork(Long userId, DunbarCircle circleSize);
    void putDefaultNetwork(Long userId, DunbarCircle circleSize, List<NetworkFriendEdgeResult> result);
    void evictDefaultNetwork(Long userId);

    Optional<List<NetworkFriendEdgeResult>> getLabelNetwork(Long userId, String labelId);
    void putLabelNetwork(Long userId, String labelId, List<NetworkFriendEdgeResult> result);
    void evictLabelNetwork(Long userId, String labelId);
    void evictAllLabelNetworks(Long userId);
}
