package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.application.dto.result.FlagDetailResult;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockCustomUser
class FlagQueryControllerTest extends BaseControllerTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Test
    @DisplayName("플래그 상세 조회 시 200과 함께 isHost 포함 응답을 반환하고 currentUserId를 전달한다")
    void getFlagDetail_Returns200WithIsHost() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        FlagSchedule schedule = FlagSchedule.of(now.plusHours(1), now.plusHours(2), now.plusHours(3));
        Flag flag = Flag.create(CURRENT_USER_ID, "테스트 플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", 1L);
        FlagDetailResult detail = FlagDetailResult.of(
                flag, new FlagUserInfo(CURRENT_USER_ID, "호스트", null), null, List.of(), true
        );
        given(flagQueryUseCase.getFlagDetail(1L, CURRENT_USER_ID)).willReturn(detail);

        // when & then
        mockMvc.perform(get("/api/v1/flags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.isHost").value(true));

        verify(flagQueryUseCase).getFlagDetail(1L, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("존재하지 않는 플래그 상세 조회 시 404를 반환한다")
    void getFlagDetail_NotFound_Returns404() throws Exception {
        // given
        given(flagQueryUseCase.getFlagDetail(999L, CURRENT_USER_ID))
                .willThrow(new FlagNotFoundException(999L));

        // when & then
        mockMvc.perform(get("/api/v1/flags/999"))
                .andExpect(status().isNotFound());
    }
}
