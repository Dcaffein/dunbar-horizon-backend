package com.example.GooRoomBe.social.adapter.in;

import com.example.GooRoomBe.global.annotation.CurrentUserId;
import com.example.GooRoomBe.social.application.dto.NetworkOneHopsByTwoHopResponse;
import com.example.GooRoomBe.social.application.dto.NetworkTwoHopSuggestionsResponse;
import com.example.GooRoomBe.social.application.dto.NetworkBetweenOneHopsResponse;
import com.example.GooRoomBe.social.application.port.in.SocialNetworkQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/network")
public class SocialNetworkController {

    private final SocialNetworkQueryUseCase socialNetworkQueryUseCase;

    @GetMapping("/one-hops")
    public ResponseEntity<List<NetworkBetweenOneHopsResponse>> getOneHopsNetwork(
            @CurrentUserId Long currentUserId
    ) {
        return ResponseEntity.ok(socialNetworkQueryUseCase.getOneHopsNetwork(currentUserId));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<NetworkTwoHopSuggestionsResponse>> getNetworkSuggestions(
            @CurrentUserId Long currentUserId,
            @RequestParam Long pivotId
    ) {
        return ResponseEntity.ok(socialNetworkQueryUseCase.getTwoHopSuggestionsByOneHop(currentUserId, pivotId));
    }

    @GetMapping("/one-hops/intersections")
    public ResponseEntity<List<NetworkOneHopsByTwoHopResponse>> getCommonOneHops(
            @CurrentUserId Long currentUserId,
            @RequestParam Long targetId
    ) {
        return ResponseEntity.ok(socialNetworkQueryUseCase.getIntersectionOneHops(currentUserId, targetId));
    }
}