package com.example.DunbarHorizon.account.adapter.in.web;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTest {

    @Test
    @WithMockCustomUser
    @DisplayName("등록된 ACTIVE 유저 이메일로 조회 시 200 OK와 프로필을 반환한다")
    void searchByEmail_ActiveUser_Returns200() throws Exception {
        // given
        String email = "found@test.com";
        UserProfileInfo profile = new UserProfileInfo(1L, "tester", null);
        given(userQueryUseCase.findActiveUserByEmail(email)).willReturn(Optional.of(profile));

        // when & then
        mockMvc.perform(get("/api/v1/users/search").param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nickname").value("tester"));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("미등록 이메일로 조회 시 404 Not Found를 반환한다")
    void searchByEmail_UserNotFound_Returns404() throws Exception {
        // given
        String email = "notfound@test.com";
        given(userQueryUseCase.findActiveUserByEmail(email)).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/v1/users/search").param("email", email))
                .andExpect(status().isNotFound());
    }
}
