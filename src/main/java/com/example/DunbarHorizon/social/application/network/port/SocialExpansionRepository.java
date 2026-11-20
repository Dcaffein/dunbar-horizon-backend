package com.example.DunbarHorizon.social.application.network.port;

import com.example.DunbarHorizon.social.application.network.dto.AnchorExpansionResult;

import java.util.List;
import java.util.Optional;

public interface SocialExpansionRepository {
    List<AnchorExpansionResult> getRecommendedNetworkByAnchor(Long meId, Long anchorId, int threshold, int limitCount);
    List<AnchorExpansionResult> getRelatedNetworkByAnchor(Long userId, Long anchorId, int thresHold, int limitCount);

    Optional<Double> findIntimacy(Long userId, Long friendId);
}
