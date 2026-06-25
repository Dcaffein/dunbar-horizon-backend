package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.dto.result.NodeGraphResult;
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
    @DisplayName("메인 홈 네트워크를 기본 크기(DUNBAR)로 조회한다")
    void getFriendsNetwork_DefaultCircleSize() throws Exception {
        given(socialNetworkQueryUseCase.getFriendsNetwork(eq(1L), eq(DunbarCircle.DUNBAR)))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/networks/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("라벨 네트워크를 조회한다")
    void getLabelNetwork_Success() throws Exception {
        String labelId = "label-1";
        given(socialNetworkQueryUseCase.getLabelNetwork(eq(1L), eq(labelId)))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/networks/labels/{labelId}", labelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
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
