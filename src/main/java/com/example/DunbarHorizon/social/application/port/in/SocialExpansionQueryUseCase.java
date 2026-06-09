package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;

import java.util.List;

public interface SocialExpansionQueryUseCase {
    List<AnchorExpansionResult> getTwoHopSuggestionsByOneHop(Long userId, Long anchorId, Double expansionValue);
    List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorFriendId, Double expansionValue);
    List<AnchorExpansionResult> getRecommendationsByAnchor(Long userId, Long anchorFriendId);
}
