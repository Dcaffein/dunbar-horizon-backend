package com.example.GooRoomBe.social.trace.api;

import com.example.GooRoomBe.social.common.dto.SocialMemberResponseDto;
import com.example.GooRoomBe.social.trace.api.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.social.trace.application.TraceService;
import com.example.GooRoomBe.support.ControllerTestSupport;
import com.example.GooRoomBe.support.WithCustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TraceController.class)
class TraceControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TraceService traceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("방문 요청 시: 공개 조건 미달성(Hidden)이면 isMatched=false를 반환한다")
    @WithCustomMockUser(userId = "visitor-id")
    void visitProfile_Hidden() throws Exception {
        // Given
        String targetId = "target-id";

        TraceRecordResponseDto responseDto = TraceRecordResponseDto.hidden();

        given(traceService.visit("visitor-id", targetId)).willReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/social/traces/{targetId}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(request -> {
                            request.setRemoteUser("visitor-id");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMatched").value(false))
                // partner가 null이면 JSON에 포함되지 않는지 확인 (@JsonInclude 동작 검증)
                .andExpect(jsonPath("$.partner").doesNotExist());
    }

    @Test
    @DisplayName("방문 요청 시: 공개 조건 달성(Revealed)이면 상대방 정보를 반환한다")
    @WithCustomMockUser(userId = "visitor-id")
    void visitProfile_Revealed() throws Exception {
        // Given
        String targetId = "target-id";

        SocialMemberResponseDto partner = new SocialMemberResponseDto(targetId, "서로통한사이");

        TraceRecordResponseDto responseDto = TraceRecordResponseDto.revealed(partner);

        given(traceService.visit("visitor-id", targetId)).willReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/social/traces/{targetId}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMatched").value(true))
                .andExpect(jsonPath("$.partner.id").value(targetId))
                .andExpect(jsonPath("$.partner.nickname").value("서로통한사이"));
    }
}