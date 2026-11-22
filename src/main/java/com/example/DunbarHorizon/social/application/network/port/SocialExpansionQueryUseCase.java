package com.example.DunbarHorizon.social.application.network.port;

import com.example.DunbarHorizon.social.application.network.dto.IntermediaryExpansionResult;

import java.util.List;

public interface SocialExpansionQueryUseCase {
    List<IntermediaryExpansionResult> getIntermediaryExpansion(Long userId, Long intermediaryFriendId, Double expansionValue);
}
