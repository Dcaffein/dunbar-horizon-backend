package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkGraphResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.dto.result.NodeEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NodeGraphResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SocialNetworkQueryServiceTest {

    @Mock
    private SocialNetworkRepository socialNetworkRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private Friendship friendship;

    @InjectMocks
    private SocialNetworkQueryService service;

    // ── getFriendsNetwork ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getFriendsNetwork: Repository의 getDefaultNetworkGraph 결과를 그대로 반환한다")
    void getFriendsNetwork_getDefaultNetworkGraph_결과를_반환한다() {
        Long userId = 1L;
        DunbarCircle circleSize = DunbarCircle.KINSHIP;
        List<NodeGraphResult> nodes = List.of(
                new NodeGraphResult(10L, 0.7, List.of(new NodeEdgeResult(20L, 0.85, 0.3))),
                new NodeGraphResult(20L, 0.3, List.of(new NodeEdgeResult(10L, 0.85, 0.7))),
                new NodeGraphResult(30L, 0.0, List.of())
        );
        NetworkGraphResult expected = new NetworkGraphResult(nodes);
        given(socialNetworkRepository.getDefaultNetworkGraph(userId, circleSize)).willReturn(expected);

        NetworkGraphResult result = service.getFriendsNetwork(userId, circleSize);

        verify(socialNetworkRepository).getDefaultNetworkGraph(userId, circleSize);
        assertThat(result).isEqualTo(expected);
    }

    // ── getLabelNetwork ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLabelNetwork: Repository에 userId와 labelId를 그대로 전달하고 결과를 반환한다")
    void getLabelNetwork_Repository에_올바른_파라미터를_전달하고_결과를_반환한다() {
        Long userId = 1L;
        String labelId = "label-abc";
        NetworkGraphResult expected = new NetworkGraphResult(List.of(
                new NodeGraphResult(10L, 0.3, List.of(new NodeEdgeResult(20L, 0.85, 0.7)))
        ));
        given(socialNetworkRepository.getLabelCustomNetwork(userId, labelId)).willReturn(expected);

        NetworkGraphResult result = service.getLabelNetwork(userId, labelId);

        verify(socialNetworkRepository).getLabelCustomNetwork(userId, labelId);
        assertThat(result).isEqualTo(expected);
    }

    // ── getNewNodeEdges ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getNewNodeEdges: skeletonIds가 null이면 빈 리스트를 반환하고 Repository를 호출하지 않는다")
    void getNewNodeEdges_skeletonIds_null이면_빈_리스트_반환() {
        assertThat(service.getNewNodeEdges(1L, 10L, null)).isEmpty();
        verifyNoInteractions(socialNetworkRepository);
    }

    @Test
    @DisplayName("getNewNodeEdges: skeletonIds가 비어있으면 빈 리스트를 반환하고 Repository를 호출하지 않는다")
    void getNewNodeEdges_skeletonIds_빈_리스트이면_빈_리스트_반환() {
        assertThat(service.getNewNodeEdges(1L, 10L, List.of())).isEmpty();
        verifyNoInteractions(socialNetworkRepository);
    }

    @Test
    @DisplayName("getNewNodeEdges: intimacy=0.5이면 dynamicLimit=7 (5 + 0.5*5)로 Repository를 호출한다")
    void getNewNodeEdges_intimacy로_dynamicLimit을_계산해서_Repository에_전달한다() {
        Long userId = 1L, targetId = 10L;
        List<Long> skeletonIds = List.of(20L, 30L);
        given(friendshipRepository.findById(Friendship.generateCompositeId(userId, targetId)))
                .willReturn(Optional.of(friendship));
        given(friendship.getIntimacy()).willReturn(0.5);
        given(socialNetworkRepository.getNewNodeEdges(userId, targetId, skeletonIds, 7)).willReturn(List.of());

        service.getNewNodeEdges(userId, targetId, skeletonIds);

        verify(socialNetworkRepository).getNewNodeEdges(userId, targetId, skeletonIds, 7);
    }

    @Test
    @DisplayName("getNewNodeEdges: me→target 친구 관계가 없으면 intimacy=0.0으로 fallback, dynamicLimit=5")
    void getNewNodeEdges_친구관계_없으면_dynamicLimit_5로_fallback() {
        Long userId = 1L, targetId = 10L;
        List<Long> skeletonIds = List.of(20L);
        given(friendshipRepository.findById(Friendship.generateCompositeId(userId, targetId)))
                .willReturn(Optional.empty());
        given(socialNetworkRepository.getNewNodeEdges(userId, targetId, skeletonIds, 5)).willReturn(List.of());

        service.getNewNodeEdges(userId, targetId, skeletonIds);

        verify(socialNetworkRepository).getNewNodeEdges(userId, targetId, skeletonIds, 5);
    }

    @Test
    @DisplayName("getNewNodeEdges: Repository 결과를 그대로 반환한다")
    void getNewNodeEdges_결과를_그대로_반환한다() {
        Long userId = 1L, targetId = 10L;
        List<Long> skeletonIds = List.of(20L);
        List<MutualFriendEdgeResult> expected = List.of(new MutualFriendEdgeResult(10L, 20L, 0.6));
        given(friendshipRepository.findById(Friendship.generateCompositeId(userId, targetId)))
                .willReturn(Optional.of(friendship));
        given(friendship.getIntimacy()).willReturn(0.8);
        given(socialNetworkRepository.getNewNodeEdges(userId, targetId, skeletonIds, 9)).willReturn(expected);

        List<MutualFriendEdgeResult> result = service.getNewNodeEdges(userId, targetId, skeletonIds);

        assertThat(result).isEqualTo(expected);
    }

    // ── getNetworkContactsOfTwoHop ─────────────────────────────────────────────

    @Test
    @DisplayName("getNetworkContactsOfTwoHop: skeletonIds가 null이면 빈 리스트를 반환하고 Repository를 호출하지 않는다")
    void getNetworkContactsOfTwoHop_skeletonIds_null이면_빈_리스트_반환() {
        assertThat(service.getNetworkContactsOfTwoHop(1L, 10L, null)).isEmpty();
        verifyNoInteractions(socialNetworkRepository);
    }

    @Test
    @DisplayName("getNetworkContactsOfTwoHop: skeletonIds가 비어있으면 빈 리스트를 반환하고 Repository를 호출하지 않는다")
    void getNetworkContactsOfTwoHop_skeletonIds_빈_리스트이면_빈_리스트_반환() {
        assertThat(service.getNetworkContactsOfTwoHop(1L, 10L, List.of())).isEmpty();
        verifyNoInteractions(socialNetworkRepository);
    }

    @Test
    @DisplayName("getNetworkContactsOfTwoHop: Repository에 skeletonIds를 전달하고 결과를 반환한다")
    void getNetworkContactsOfTwoHop_결과를_그대로_반환한다() {
        Long userId = 1L, targetId = 10L;
        List<Long> skeletonIds = List.of(20L, 30L);
        List<NetworkOneHopsByTwoHopResult> expected = List.of(new NetworkOneHopsByTwoHopResult(5L));
        given(socialNetworkRepository.getNetworkContactsOfTwoHop(userId, targetId, skeletonIds)).willReturn(expected);

        List<NetworkOneHopsByTwoHopResult> result = service.getNetworkContactsOfTwoHop(userId, targetId, skeletonIds);

        verify(socialNetworkRepository).getNetworkContactsOfTwoHop(userId, targetId, skeletonIds);
        assertThat(result).isEqualTo(expected);
    }
}
