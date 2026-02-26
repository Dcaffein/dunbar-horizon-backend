package com.example.GooRoomBe.social.application.service;

import com.example.GooRoomBe.social.application.dto.NetworkBetweenOneHopsResponse;
import com.example.GooRoomBe.social.application.dto.NetworkOneHopsByTwoHopResponse;
import com.example.GooRoomBe.social.application.dto.NetworkTwoHopSuggestionsResponse;
import com.example.GooRoomBe.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.GooRoomBe.social.application.port.out.SocialNetworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialNetworkQueryService implements SocialNetworkQueryUseCase {

    private static final int MIN_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final double MIN_VALUE = 0.0;
    private static final double MAX_VALUE = 1.0;

    private final SocialNetworkRepository socialNetworkRepository;

    @Override
    public List<NetworkBetweenOneHopsResponse> getOneHopsNetwork(Long userId) {
        return socialNetworkRepository.getOneHopsNetwork(userId);
    }

    @Override
    public List<NetworkOneHopsByTwoHopResponse> getIntersectionOneHops(Long userId, Long targetId) {
        return socialNetworkRepository.getIntersectionOneHops(userId, targetId);
    }

    @Override
    public List<NetworkTwoHopSuggestionsResponse> getTwoHopSuggestionsByOneHop(Long userId, Long pivotId) {
        return socialNetworkRepository.getTwoHopSuggestionsByOneHop(userId, pivotId);
    }

    @Override
    public Set<Long> getPivotExpansion(Long userId, Long pivotFriendId, Double expansionValue) {
        validateExpansionValue(expansionValue);
        int limitCount = calculateLimit(expansionValue);

        // 분석용 'Network' 메서드를 호출하여 확장된 인맥을 반환
        return socialNetworkRepository.getRelatedNetworkByPivot(userId, pivotFriendId, limitCount);
    }

    private void validateExpansionValue(Double value) {
        if (value == null || value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException(
                    String.format("expansionValue는 %.1f에서 %.1f 사이여야 합니다. 입력값: %s",
                            MIN_VALUE, MAX_VALUE, value)
            );
        }
    }

    /**
     * expansionValue($v$)에 따른 탐색 제한 수($L$) 계산 공식:
     * $$L = L_{min} + v \times (L_{max} - L_{min})$$
     */
    private int calculateLimit(Double value) {
        double calculated = MIN_LIMIT + (value * (MAX_LIMIT - MIN_LIMIT));
        return (int) Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, Math.round(calculated)));
    }
}