package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkGraphResult;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockCustomUser
class SocialQueryControllerTest extends BaseControllerTest {

    @Test
    @DisplayName("expansionValue를 전달하면 앵커 기반 2-hop 새 친구 추천 목록을 조회한다")
    void getTwoHopSuggestionsByAnchor_Success() throws Exception {
        Long anchorId = 2L;
        double expansionValue = 0.5;
        AnchorExpansionResult suggestion = new AnchorExpansionResult(3L, "추천유저", 0.5, 2, 1);
        given(socialExpansionQueryUseCase.getTwoHopSuggestionsByOneHop(eq(1L), eq(anchorId), eq(expansionValue)))
                .willReturn(List.of(suggestion));

        mockMvc.perform(get("/api/v1/networks/suggestions/anchor")
                        .param("anchorId", String.valueOf(anchorId))
                        .param("expansionValue", String.valueOf(expansionValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].nickname").value("추천유저"));
    }

    @Test
    @DisplayName("메인 홈 네트워크를 기본 크기(DUNBAR)로 조회한다")
    void getFriendsNetwork_DefaultCircleSize() throws Exception {
        given(socialNetworkQueryUseCase.getFriendsNetwork(eq(1L), eq(DunbarCircle.DUNBAR)))
                .willReturn(new NetworkGraphResult(List.of()));

        mockMvc.perform(get("/api/v1/networks/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray());
    }

    @Test
    @DisplayName("앵커의 intimacy 기반으로 동적 파라미터를 적용한 추천 목록을 조회한다")
    void getAnchorRecommendation_Success() throws Exception {
        Long anchorId = 2L;
        given(socialExpansionQueryUseCase.getRecommendationsByAnchor(eq(1L), eq(anchorId))).willReturn(List.of());

        mockMvc.perform(get("/api/v1/networks/recommendations")
                        .param("anchorId", String.valueOf(anchorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
