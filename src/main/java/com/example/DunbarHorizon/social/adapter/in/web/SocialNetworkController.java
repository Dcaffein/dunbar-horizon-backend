package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.social.application.network.dto.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.network.dto.ConnectionPathResult;
import com.example.DunbarHorizon.social.application.network.dto.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.network.dto.NodeGraphResult;
import com.example.DunbarHorizon.social.application.network.SocialConnectionPathQueryService;
import com.example.DunbarHorizon.social.application.network.SocialExpansionQueryService;
import com.example.DunbarHorizon.social.application.network.SocialNetworkQueryService;
import com.example.DunbarHorizon.social.domain.network.DunbarCircle;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/network")
public class SocialNetworkController {

    private final SocialExpansionQueryService socialExpansionQueryService;
    private final SocialNetworkQueryService socialNetworkQueryService;
    private final SocialConnectionPathQueryService socialConnectionPathQueryService;

    /**
     * 메인 홈 네트워크 (Soft Morphing 적용으로 1개의 API로 통합)
     * 프론트엔드는 슬라이더 위치에 따라 circleSize(SUPPORT, SYMPATHY, KINSHIP, DUNBAR)를 보냄
     */
    @GetMapping
    public ResponseEntity<List<NodeGraphResult>> getFriendsNetwork(
            @CurrentUserId Long currentUserId,
            @RequestParam(defaultValue = "DUNBAR") DunbarCircle circleSize
    ) {
        return ResponseEntity.ok(socialNetworkQueryService.getFriendsNetwork(currentUserId, circleSize));
    }

    /**
     * 라벨 네트워크
     * 라벨 크기와 무관하게 최대 150명(Dunbar's Ceiling)까지 단일 뷰로 제공
     */
    @GetMapping("/labels/{labelId}")
    public ResponseEntity<List<NodeGraphResult>> getLabelNetwork(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelId
    ) {
        return ResponseEntity.ok(socialNetworkQueryService.getLabelNetwork(currentUserId, labelId));
    }

    /**
     * 1-Hop 친구를 기존 네트워크에 수동 추가
     * 클라이언트가 현재 화면의 skeleton ID 목록을 전달하여 동적 컨텍스트 반영 및 보안 검증
     */
    @GetMapping("/edges")
    public ResponseEntity<List<MutualFriendEdgeResult>> getNetworkEdges(
            @CurrentUserId Long currentUserId,
            @RequestParam Long targetId,
            @RequestParam List<Long> baseNetworkFriendIds
    ) {
        return ResponseEntity.ok(socialNetworkQueryService.getNetworkEdges(currentUserId, targetId, baseNetworkFriendIds));
    }

    /**
     * 2-Hop 유저와 기존 네트워크와의 접점
     * 클라이언트가 현재 화면의 skeleton ID 목록을 전달하여 동적 컨텍스트 반영 및 보안 검증
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<AnchorExpansionResult>> getTwoHopRecommendation(
            @CurrentUserId Long currentUserId,
            @RequestParam Long anchorId
    ) {
        return ResponseEntity.ok(socialExpansionQueryService.getRecommendationsByAnchor(currentUserId, anchorId));
    }

    @GetMapping("/path")
    public ResponseEntity<ConnectionPathResult> getConnectionPath(
            @CurrentUserId Long currentUserId,
            @RequestParam Long targetId
    ) {
        return ResponseEntity.ok(socialConnectionPathQueryService.getConnectionPath(currentUserId, targetId));
    }
}
