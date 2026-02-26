package com.example.GooRoomBe.social.application.port.out;

import com.example.GooRoomBe.social.application.dto.NetworkBetweenOneHopsResponse;
import com.example.GooRoomBe.social.application.dto.NetworkOneHopsByTwoHopResponse;
import com.example.GooRoomBe.social.application.dto.NetworkTwoHopSuggestionsResponse;

import java.util.List;
import java.util.Set;

public interface SocialNetworkRepository {
    List<NetworkBetweenOneHopsResponse> getOneHopsNetwork(Long userId);
    List<NetworkTwoHopSuggestionsResponse> getTwoHopSuggestionsByOneHop(Long userId, Long pivotId);
    List<NetworkOneHopsByTwoHopResponse> getIntersectionOneHops(Long userId, Long targetId);
    Set<Long> getRelatedNetworkByPivot(Long userId, Long pivotFriendId, int limitCount);
}
