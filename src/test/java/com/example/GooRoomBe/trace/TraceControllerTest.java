package com.example.GooRoomBe.trace;

import com.example.GooRoomBe.support.BaseControllerTest;
import com.example.GooRoomBe.support.WithMockCustomUser;
import com.example.GooRoomBe.trace.adapter.in.web.TraceController;
import com.example.GooRoomBe.trace.adapter.in.web.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.trace.adapter.in.web.dto.VisitRequestDto;
import com.example.GooRoomBe.trace.application.TraceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TraceController.class)
@WithMockCustomUser(id = "1") // 1번 유저로 로그인한 상황 시뮬레이션
class TraceControllerTest extends BaseControllerTest {

    @MockitoBean // Spring Boot 3.4+ 기준 (이전 버전은 @MockBean)
    private TraceService traceService;

    @Test
    @DisplayName("상대방 프로필 방문 시 기록을 남기고 응답을 반환한다")
    void visitProfile_Success() throws Exception {
        // given
        Long targetId = 2L;
        VisitRequestDto requestDto = new VisitRequestDto(targetId);

        // 서비스 응답 Mocking (정체는 숨겨진 상태라고 가정)
        TraceRecordResponseDto mockResponse = TraceRecordResponseDto.hidden();

        given(traceService.recordTrace(eq(1L), eq(targetId)))
                .willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/social/traces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));

        // 서비스가 로그인 유저 ID(1L)와 타겟 ID(2L)로 호출되었는지 검증
        verify(traceService).recordTrace(1L, targetId);
    }
}