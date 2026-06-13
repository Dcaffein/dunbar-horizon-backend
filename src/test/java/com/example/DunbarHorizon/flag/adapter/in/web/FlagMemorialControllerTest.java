package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.application.dto.result.MemorialListResult;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockCustomUser
class FlagMemorialControllerTest extends BaseControllerTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Test
    @DisplayName("추도문 생성 시 201을 반환하고 createMemorial()를 호출한다")
    void createMemorial_Returns201() throws Exception {
        given(flagMemorialCommandUseCase.createMemorial(eq(1L), eq(CURRENT_USER_ID), anyString()))
                .willReturn(10L);
        String body = """
                {"content": "추도문 내용"}
                """;

        mockMvc.perform(post("/api/v1/flags/1/memorials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(flagMemorialCommandUseCase).createMemorial(1L, CURRENT_USER_ID, "추도문 내용");
    }

    @Test
    @DisplayName("추도문 목록 조회 시 200을 반환하고 getMemorials()를 호출한다")
    void getMemorials_Returns200() throws Exception {
        given(flagMemorialQueryUseCase.getMemorials(1L, CURRENT_USER_ID)).willReturn(MemorialListResult.empty());

        mockMvc.perform(get("/api/v1/flags/1/memorials"))
                .andExpect(status().isOk());

        verify(flagMemorialQueryUseCase).getMemorials(1L, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("추도문 수정 시 200을 반환하고 updateMemorial()를 호출한다")
    void updateMemorial_Returns200() throws Exception {
        String body = """
                {"content": "수정된 추도문 내용"}
                """;

        mockMvc.perform(patch("/api/v1/flags/memorials/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(flagMemorialCommandUseCase).updateMemorial(1L, CURRENT_USER_ID, "수정된 추도문 내용");
    }

    @Test
    @DisplayName("추도문 삭제 시 204를 반환하고 deleteMemorial()를 호출한다")
    void deleteMemorial_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/flags/memorials/1"))
                .andExpect(status().isNoContent());

        verify(flagMemorialCommandUseCase).deleteMemorial(1L, CURRENT_USER_ID);
    }
}
