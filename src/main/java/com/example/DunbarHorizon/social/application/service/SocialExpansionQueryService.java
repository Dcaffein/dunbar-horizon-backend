package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.in.SocialExpansionQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialExpansionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialExpansionQueryService implements SocialExpansionQueryUseCase {

    private static final double MIN_VALUE = 0.0;
    private static final double INFLECTION_VALUE = 0.8;
    private static final double MAX_VALUE = 1.0;

    private static final int MIN_LIMIT = 10;
    private static final int INFLECTION_LIMIT = 50;
    private static final int MAX_LIMIT = 150;

    private static final int MAX_THRESHOLD = 5;
    private static final int INFLECTION_THRESHOLD = 2;
    private static final int MIN_THRESHOLD = 1;

    private final SocialExpansionRepository socialExpansionRepository;

    @Override
    public List<AnchorExpansionResult> getTwoHopSuggestionsByOneHop(Long userId, Long pivotId) {
        return socialExpansionRepository.getRecommendedNetworkByAnchor(userId, pivotId, 1  ,10);
    }

    @Override
    public List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorFriendId, Double expansionValue) {
        validateExpansionValue(expansionValue);

        int limitCount = calculateLimit(expansionValue);
        int threshold = calculateThreshold(expansionValue);

        return socialExpansionRepository.getRelatedNetworkByAnchor(userId, anchorFriendId, limitCount, threshold);
    }

    private void validateExpansionValue(Double value) {
        if (value == null || value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException(
                    String.format("expansionValue는 %.1f에서 %.1f 사이여야 합니다. 입력값: %s",
                            MIN_VALUE, MAX_VALUE, value)
            );
        }
    }


    private int calculateLimit(Double v) {
        if (v <= INFLECTION_VALUE) {
            double ratio = (v - MIN_VALUE) / (INFLECTION_VALUE - MIN_VALUE);
            return (int) Math.round(MIN_LIMIT + ratio * (INFLECTION_LIMIT - MIN_LIMIT));
        } else {
            double ratio = (v - INFLECTION_VALUE) / (MAX_VALUE - INFLECTION_VALUE);
            return (int) Math.round(INFLECTION_LIMIT + ratio * (MAX_LIMIT - INFLECTION_LIMIT));
        }
    }

    private int calculateThreshold(Double v) {
        if (v <= INFLECTION_VALUE) {
            double ratio = (v - MIN_VALUE) / (INFLECTION_VALUE - MIN_VALUE);
            return (int) Math.round(MAX_THRESHOLD - ratio * (MAX_THRESHOLD - INFLECTION_THRESHOLD));
        } else {
            double ratio = (v - INFLECTION_VALUE) / (MAX_VALUE - INFLECTION_VALUE);
            return (int) Math.round(INFLECTION_THRESHOLD - ratio * (INFLECTION_THRESHOLD - MIN_THRESHOLD));
        }
    }
}