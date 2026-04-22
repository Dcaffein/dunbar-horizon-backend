package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.port.in.SocialExpansionQueryUseCase;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/networks")
public class SocialQueryController {

    private final SocialExpansionQueryUseCase expansionQueryUseCase;
    private final SocialNetworkQueryUseCase networkQueryUseCase;

    @GetMapping("/me")
    public ResponseEntity<List<NetworkFriendEdgeResult>> getFriendsNetwork(
            @CurrentUserId Long currentUserId
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getFriendsNetwork(currentUserId));
    }

    @GetMapping("/verified")
    public ResponseEntity<List<NetworkFriendEdgeResult>> getVerifiedFriendsNetwork(
            @CurrentUserId Long currentUserId,
            @RequestParam List<Long> targetIds
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getVerifiedFriendsNetwork(currentUserId, targetIds));
    }

    @GetMapping("/top/intimacy")
    public ResponseEntity<List<NetworkFriendEdgeResult>> getTopIntimateFriendsNetwork(
            @CurrentUserId Long currentUserId
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getTopIntimateFriendsNetwork(currentUserId));
    }

    @GetMapping("/top/interest")
    public ResponseEntity<List<NetworkFriendEdgeResult>> getTopInterestedFriendsNetwork(
            @CurrentUserId Long currentUserId
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getTopInterestFriendsNetwork(currentUserId));
    }

    @GetMapping("/mutual/one-hop")
    public ResponseEntity<List<MutualFriendEdgeResult>> getOneHopMutualFriendEdges(
            @CurrentUserId Long currentUserId,
            @RequestParam Long targetId
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getIntersectionByOneHop(currentUserId, targetId));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<AnchorExpansionResult>> getTwoHopRecommendation(
            @CurrentUserId Long currentUserId,
            @RequestParam Long anchorId
    ) {
        return ResponseEntity.ok(expansionQueryUseCase.getAnchorExpansion(currentUserId, anchorId, 0.3));
    }
}