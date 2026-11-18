package com.example.DunbarHorizon.social.application.network.port;

import com.example.DunbarHorizon.social.application.network.dto.AnchorExpansionResult;

import java.util.List;

public interface SocialExpansionQueryUseCase {
    List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorFriendId, Double expansionValue);
}
