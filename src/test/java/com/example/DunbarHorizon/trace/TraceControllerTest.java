package com.example.DunbarHorizon.trace;

import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import com.example.DunbarHorizon.trace.adapter.in.web.TraceController;
import com.example.DunbarHorizon.trace.adapter.in.web.dto.VisitRequestDto;
import com.example.DunbarHorizon.trace.application.TraceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TraceController.class)
@WithMockCustomUser(id = "1")
class TraceControllerTest extends BaseControllerTest {

    @MockitoBean
    private TraceService traceService;

    @Test
    @DisplayName("상대방 프로필 방문 시 기록을 남기고 200 OK를 반환한다")
    void visitProfile_Success() throws Exception {
        // given
        Long targetId = 2L;
        VisitRequestDto requestDto = new VisitRequestDto(targetId);
        doNothing().when(traceService).recordTrace(eq(1L), eq(targetId));

        // when & then
        mockMvc.perform(post("/api/v1/social/traces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(traceService).recordTrace(1L, targetId);
    }
}
