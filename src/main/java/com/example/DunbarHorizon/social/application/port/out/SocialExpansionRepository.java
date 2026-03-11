package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;

import java.util.List;

public interface SocialExpansionRepository {
    List<AnchorExpansionResult> getRecommendedNetworkByAnchor(Long meId, Long anchorId, int threshold, int limitCount);
    List<AnchorExpansionResult> getRelatedNetworkByAnchor(Long userId, Long anchorId, int thresHold, int limitCount);
}
