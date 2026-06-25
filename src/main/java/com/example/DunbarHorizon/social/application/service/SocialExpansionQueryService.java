package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.port.in.SocialExpansionQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialExpansionRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class SocialExpansionQueryService implements SocialExpansionQueryUseCase {

    private static final double MIN_VALUE = 0.0;
    private static final double INFLECTION_VALUE = 0.8;
    private static final double MAX_VALUE = 1.0;

    private static final int BUZZ_MIN_LIMIT        = 10;
    private static final int BUZZ_INFLECTION_LIMIT = 50;
    private static final int BUZZ_MAX_LIMIT        = 150;

    private static final int REC_MIN_LIMIT        = 2;
    private static final int REC_INFLECTION_LIMIT = 10;
    private static final int REC_MAX_LIMIT        = 15;

    private static final int MAX_THRESHOLD        = 5;
    private static final int INFLECTION_THRESHOLD = 2;
    private static final int MIN_THRESHOLD        = 1;

    private final SocialExpansionRepository socialExpansionRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    public List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorFriendId, Double expansionValue) {
        validateExpansionValue(expansionValue);
        int limit = calculateLimit(expansionValue, BUZZ_MIN_LIMIT, BUZZ_INFLECTION_LIMIT, BUZZ_MAX_LIMIT);
        int threshold = calculateThreshold(expansionValue);
        return socialExpansionRepository.getRelatedNetworkByAnchor(userId, anchorFriendId, threshold, limit);
    }

    @Override
    public List<AnchorExpansionResult> getRecommendationsByAnchor(Long userId, Long anchorFriendId) {
        double intimacy = friendshipRepository
                .findById(Friendship.generateCompositeId(userId, anchorFriendId))
                .orElseThrow(() -> new FriendshipNotFoundException(userId, anchorFriendId))
                .getIntimacy();
        int limit = calculateLimit(intimacy, REC_MIN_LIMIT, REC_INFLECTION_LIMIT, REC_MAX_LIMIT);
        int threshold = calculateThreshold(intimacy);
        return socialExpansionRepository.getRecommendedNetworkByAnchor(userId, anchorFriendId, threshold, limit);
    }

    private void validateExpansionValue(Double value) {
        if (value == null || value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException(
                    String.format("expansionValue는 %.1f에서 %.1f 사이여야 합니다. 입력값: %s",
                            MIN_VALUE, MAX_VALUE, value)
            );
        }
    }


    private int calculateLimit(Double v, int min, int inflection, int max) {
        if (v <= INFLECTION_VALUE) {
            double ratio = (v - MIN_VALUE) / (INFLECTION_VALUE - MIN_VALUE);
            return (int) Math.round(min + ratio * (inflection - min));
        } else {
            double ratio = (v - INFLECTION_VALUE) / (MAX_VALUE - INFLECTION_VALUE);
            return (int) Math.round(inflection + ratio * (max - inflection));
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