package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.in.SocialExpansionQueryUseCase;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
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

    /**
     * 메인 홈 네트워크 (Soft Morphing 적용으로 1개의 API로 통합)
     * 프론트엔드는 슬라이더 위치에 따라 circleSize(SUPPORT, SYMPATHY, KINSHIP, DUNBAR)를 보냄
     */
    @GetMapping("/me")
    public ResponseEntity<List<NetworkFriendEdgeResult>> getFriendsNetwork(
            @CurrentUserId Long currentUserId,
            @RequestParam(defaultValue = "DUNBAR") DunbarCircle circleSize
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getFriendsNetwork(currentUserId, circleSize.getLimitSize()));
    }

    /**
     * 라벨 네트워크
     * 라벨은 기본적으로 50명(KINSHIP)까지만 방어적으로 조회하도록 defaultValue 설정
     */
    @GetMapping("/labels/{labelName}")
    public ResponseEntity<List<NetworkFriendEdgeResult>> getLabelNetwork(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelName,
            @RequestParam(defaultValue = "KINSHIP") DunbarCircle circleSize
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getLabelNetwork(currentUserId, labelName, circleSize.getLimitSize()));
    }

    /**
     * 1-Hop 친구 수동 추가 (Drag & Drop)
     * 클라이언트가 현재 보고 있는 화면의 컨텍스트(labelName, circleSize)를 전달하여 스켈레톤 붕괴 방지
     */
    @GetMapping("/mutual/one-hop")
    public ResponseEntity<List<MutualFriendEdgeResult>> getOneHopMutualFriendEdges(
            @CurrentUserId Long currentUserId,
            @RequestParam Long targetId,
            @RequestParam(required = false) String labelName,
            @RequestParam(defaultValue = "DUNBAR") DunbarCircle circleSize
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getIntersectionByOneHop(
                currentUserId, targetId, labelName, circleSize.getLimitSize()
        ));
    }

    /**
     * 2-Hop 유저 추천 클릭
     * 클라이언트가 현재 보고 있는 화면의 컨텍스트(labelName, circleSize) 기준 겹치는 지인만 도출 + 이방인 페널티 적용
     */
    @GetMapping("/mutual/two-hop")
    public ResponseEntity<List<NetworkOneHopsByTwoHopResult>> getTwoHopMutualFriends(
            @CurrentUserId Long currentUserId,
            @RequestParam Long targetId,
            @RequestParam(required = false) String labelName,
            @RequestParam(defaultValue = "DUNBAR") DunbarCircle circleSize
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getIntersectionByTwoHop(
                currentUserId, targetId, labelName, circleSize.getLimitSize()
        ));
    }

    /**
     * 추천 유저 앵커 확장
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<AnchorExpansionResult>> getTwoHopRecommendation(
            @CurrentUserId Long currentUserId,
            @RequestParam Long anchorId
    ) {
        return ResponseEntity.ok(expansionQueryUseCase.getAnchorExpansion(currentUserId, anchorId, 0.3));
    }
}