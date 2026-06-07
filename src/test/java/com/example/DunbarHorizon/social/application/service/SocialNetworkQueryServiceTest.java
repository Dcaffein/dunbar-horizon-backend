package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialNetworkQueryServiceTest {

    @Mock
    private SocialNetworkRepository socialNetworkRepository;

    @InjectMocks
    private SocialNetworkQueryService service;

    @Test
    @DisplayName("getFriendsNetwork: Repository에 userId와 DunbarCircle을 그대로 전달하고 결과를 반환한다")
    void getFriendsNetwork_Repository에_올바른_파라미터를_전달하고_결과를_반환한다() {
        // given
        Long userId = 1L;
        DunbarCircle circleSize = DunbarCircle.KINSHIP;
        List<NetworkFriendEdgeResult> expected = List.of(
                new NetworkFriendEdgeResult(10L, 20L, 0.8, 0.5, 0.4)
        );
        given(socialNetworkRepository.getDefaultIntimacyNetwork(userId, circleSize)).willReturn(expected);

        // when
        List<NetworkFriendEdgeResult> result = service.getFriendsNetwork(userId, circleSize);

        // then
        verify(socialNetworkRepository).getDefaultIntimacyNetwork(userId, circleSize);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("getLabelNetwork: Repository에 userId와 labelId를 그대로 전달하고 결과를 반환한다")
    void getLabelNetwork_Repository에_올바른_파라미터를_전달하고_결과를_반환한다() {
        // given
        Long userId = 1L;
        String labelId = "label-abc";
        List<NetworkFriendEdgeResult> expected = List.of(
                new NetworkFriendEdgeResult(10L, 20L, 0.7, 0.3, 0.4)
        );
        given(socialNetworkRepository.getLabelCustomNetwork(userId, labelId)).willReturn(expected);

        // when
        List<NetworkFriendEdgeResult> result = service.getLabelNetwork(userId, labelId);

        // then
        verify(socialNetworkRepository).getLabelCustomNetwork(userId, labelId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("getNewNodeEdges: Repository에 파라미터를 그대로 전달하고 결과를 반환한다")
    void getNewNodeEdges_Repository에_올바른_파라미터를_전달하고_결과를_반환한다() {
        // given
        Long userId = 1L;
        Long targetId = 99L;
        String labelName = "친구들";
        int limitSize = DunbarCircle.KINSHIP.getLimitSize();
        List<MutualFriendEdgeResult> expected = List.of(
                new MutualFriendEdgeResult(99L, 20L, 0.6)
        );
        given(socialNetworkRepository.getNewNodeEdges(userId, targetId, labelName, limitSize))
                .willReturn(expected);

        // when
        List<MutualFriendEdgeResult> result = service.getNewNodeEdges(userId, targetId, labelName, limitSize);

        // then
        verify(socialNetworkRepository).getNewNodeEdges(userId, targetId, labelName, limitSize);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("getNetworkContactsOfTwoHop: Repository에 파라미터를 그대로 전달하고 결과를 반환한다")
    void getNetworkContactsOfTwoHop_Repository에_올바른_파라미터를_전달하고_결과를_반환한다() {
        // given
        Long userId = 1L;
        Long targetId = 99L;
        String labelName = null;
        int limitSize = DunbarCircle.DUNBAR.getLimitSize();
        List<NetworkOneHopsByTwoHopResult> expected = List.of(
                new NetworkOneHopsByTwoHopResult(5L)
        );
        given(socialNetworkRepository.getNetworkContactsOfTwoHop(userId, targetId, labelName, limitSize))
                .willReturn(expected);

        // when
        List<NetworkOneHopsByTwoHopResult> result = service.getNetworkContactsOfTwoHop(userId, targetId, labelName, limitSize);

        // then
        verify(socialNetworkRepository).getNetworkContactsOfTwoHop(userId, targetId, labelName, limitSize);
        assertThat(result).isEqualTo(expected);
    }
}
