package com.example.DunbarHorizon.social.application.network.port;

import com.example.DunbarHorizon.social.application.network.dto.IntermediaryExpansionResult;

import java.util.List;
import java.util.Optional;

public interface SocialExpansionRepository {
    List<IntermediaryExpansionResult> getRecommendedNetworkByIntermediary(Long meId, Long intermediaryId, int threshold, int limitCount);
    List<IntermediaryExpansionResult> getRelatedNetworkByIntermediary(Long userId, Long intermediaryId, int thresHold, int limitCount);

    Optional<Double> findIntimacy(Long userId, Long friendId);
}
