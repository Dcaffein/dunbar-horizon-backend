package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.application.dto.result.FlagDetailResult;
import com.example.DunbarHorizon.flag.application.port.in.FlagRole;
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

    @Test
    @DisplayName("친구 플래그 조회 시 200을 반환하고 getFriendFlags()를 호출한다")
    void getFriendFlags_Returns200() throws Exception {
        given(flagQueryUseCase.getFriendFlags(CURRENT_USER_ID)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/flags/friends"))
                .andExpect(status().isOk());

        verify(flagQueryUseCase).getFriendFlags(CURRENT_USER_ID);
    }

    @Test
    @DisplayName("내 플래그 조회(HOST) 시 200을 반환하고 getFlagsByRole()를 호출한다")
    void getMyFlags_HostRole_Returns200() throws Exception {
        given(flagQueryUseCase.getFlagsByRole(CURRENT_USER_ID, FlagRole.HOST)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/flags/me").param("role", "HOST"))
                .andExpect(status().isOk());

        verify(flagQueryUseCase).getFlagsByRole(CURRENT_USER_ID, FlagRole.HOST);
    }

    @Test
    @DisplayName("특정 유저 최근 플래그 조회 시 200을 반환하고 getRecentFlags()를 호출한다")
    void getRecentFlags_Returns200() throws Exception {
        Long targetUserId = 2L;
        given(flagQueryUseCase.getRecentFlags(targetUserId)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/flags/users/{userId}/recent", targetUserId))
                .andExpect(status().isOk());

        verify(flagQueryUseCase).getRecentFlags(targetUserId);
    }

    @Test
    @DisplayName("특정 유저 플래그 조회(PARTICIPANT) 시 200을 반환하고 getFlagsByRole()를 호출한다")
    void getUserFlags_ParticipantRole_Returns200() throws Exception {
        Long targetUserId = 2L;
        given(flagQueryUseCase.getFlagsByRole(targetUserId, FlagRole.PARTICIPANT)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/flags/users/{userId}", targetUserId).param("role", "PARTICIPANT"))
                .andExpect(status().isOk());

        verify(flagQueryUseCase).getFlagsByRole(targetUserId, FlagRole.PARTICIPANT);
    }
}
