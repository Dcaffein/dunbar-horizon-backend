package com.example.DunbarHorizon.social.application.network.port;

import com.example.DunbarHorizon.social.application.network.dto.AnchorExpansionResult;

import java.util.List;

public interface SocialExpansionRepository {
    List<AnchorExpansionResult> getRecommendedNetworkByAnchor(Long meId, Long anchorId, int threshold, int limitCount);
    List<AnchorExpansionResult> getRelatedNetworkByAnchor(Long userId, Long anchorId, int thresHold, int limitCount);
}
