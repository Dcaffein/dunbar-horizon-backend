package com.example.GooRoomBe.social.application.port.in;

import com.example.GooRoomBe.social.application.dto.NetworkOneHopsByTwoHopResponse;
import com.example.GooRoomBe.social.application.dto.NetworkTwoHopSuggestionsResponse;
import com.example.GooRoomBe.social.application.dto.NetworkBetweenOneHopsResponse;

import java.util.List;
import java.util.Set;

public interface SocialNetworkQueryUseCase {
    List<NetworkBetweenOneHopsResponse> getOneHopsNetwork(Long userId);
    List<NetworkOneHopsByTwoHopResponse> getIntersectionOneHops(Long userId, Long targetId);
    List<NetworkTwoHopSuggestionsResponse> getTwoHopSuggestionsByOneHop(Long userId, Long pivotId);
    Set<Long> getPivotExpansion(Long userId, Long pivotFriendId, Double expansionValue);
}