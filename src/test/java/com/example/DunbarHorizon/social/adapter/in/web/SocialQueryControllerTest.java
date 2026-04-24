package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.port.in.SocialExpansionQueryUseCase;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SocialQueryController.class)
@WithMockCustomUser()
class SocialQueryControllerTest extends BaseControllerTest {

    @MockitoBean
    private SocialExpansionQueryUseCase expansionQueryUseCase;

    @MockitoBean
    private SocialNetworkQueryUseCase networkQueryUseCase;

    @Test
    @DisplayName("피벗 친구 기반 2-hop 새 친구 추천 목록을 조회한다")
    void getTwoHopSuggestionsByPivot_Success() throws Exception {
        // given
        Long pivotId = 2L;
        AnchorExpansionResult suggestion = new AnchorExpansionResult(3L, "추천유저", 0.5, 2, 1);
        given(expansionQueryUseCase.getTwoHopSuggestionsByOneHop(eq(1L), eq(pivotId)))
                .willReturn(List.of(suggestion));

        // when & then
        mockMvc.perform(get("/api/v1/networks/suggestions/pivot")
                        .param("pivotId", String.valueOf(pivotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].nickname").value("추천유저"));
    }

    @Test
    @DisplayName("메인 홈 네트워크를 기본 크기(DUNBAR)로 조회한다")
    void getFriendsNetwork_DefaultCircleSize() throws Exception {
        // given
        given(networkQueryUseCase.getFriendsNetwork(eq(1L), eq(150))).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/networks/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("앵커 확장 추천 목록을 조회한다")
    void getAnchorRecommendation_Success() throws Exception {
        // given
        Long anchorId = 2L;
        given(expansionQueryUseCase.getAnchorExpansion(eq(1L), eq(anchorId), eq(0.3))).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/networks/recommendations")
                        .param("anchorId", String.valueOf(anchorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
