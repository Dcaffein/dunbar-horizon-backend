package com.example.GooRoomBe.trace.api;

import com.example.GooRoomBe.social.common.dto.SocialMemberResponseDto;
import com.example.GooRoomBe.trace.api.TraceController;
import com.example.GooRoomBe.trace.api.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.trace.api.dto.VisitRequestDto;
import com.example.GooRoomBe.trace.application.TraceService;
import com.example.GooRoomBe.support.ControllerTestSupport;
import com.example.GooRoomBe.support.WithCustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TraceController.class)
class TraceControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TraceService traceService;

    @Test
    @DisplayName("방문 요청 시: 공개 조건 미달성(Hidden)이면 isMatched=false를 반환한다")
    @WithCustomMockUser(userId = "visitor-id")
    void visitProfile_Hidden() throws Exception {
        // Given
        String targetId = "target-id";

        VisitRequestDto request = new VisitRequestDto(targetId);

        TraceRecordResponseDto responseDto = TraceRecordResponseDto.hidden();

        given(traceService.visit("visitor-id", targetId)).willReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/social/traces")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMatched").value(false))
                .andExpect(jsonPath("$.partner").doesNotExist());
    }

    @Test
    @DisplayName("방문 요청 시: 공개 조건 달성(Revealed)이면 상대방 정보를 반환한다")
    @WithCustomMockUser(userId = "visitor-id")
    void visitProfile_Revealed() throws Exception {
        // Given
        String targetId = "target-id";

        VisitRequestDto request = new VisitRequestDto(targetId);

        SocialMemberResponseDto partner = new SocialMemberResponseDto(targetId, "서로통한사이");
        TraceRecordResponseDto responseDto = TraceRecordResponseDto.revealed(partner);

        given(traceService.visit("visitor-id", targetId)).willReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/social/traces")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMatched").value(true))
                .andExpect(jsonPath("$.partner.id").value(targetId))
                .andExpect(jsonPath("$.partner.nickname").value("서로통한사이"));
    }
}