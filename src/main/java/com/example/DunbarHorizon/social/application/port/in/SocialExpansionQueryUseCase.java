package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;

import java.util.List;

public interface SocialExpansionQueryUseCase {
    List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorFriendId, Double expansionValue);
    List<AnchorExpansionResult> getRecommendationsByAnchor(Long userId, Long anchorFriendId);
}
