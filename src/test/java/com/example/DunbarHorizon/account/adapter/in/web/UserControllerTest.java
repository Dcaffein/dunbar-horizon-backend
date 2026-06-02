package com.example.DunbarHorizon.account.adapter.in.web;

import com.example.DunbarHorizon.account.application.dto.MyProfileResult;
import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.domain.exception.UserNotFoundException;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockCustomUser
class UserControllerTest extends BaseControllerTest {

    @Test
    @DisplayName("로그인한 유저가 자신의 프로필을 조회하면 email 포함 전체 정보를 반환한다")
    void getMyProfile_Success() throws Exception {
        // given
        MyProfileResult profile = new MyProfileResult(1L, "me@test.com", "나", "https://img.com/me.png");
        given(userQueryUseCase.getMyProfile(1L)).willReturn(profile);

        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("me@test.com"))
                .andExpect(jsonPath("$.nickname").value("나"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://img.com/me.png"));
    }

    @Test
    @DisplayName("로그인한 유저가 존재하지 않으면 404를 반환한다")
    void getMyProfile_UserNotFound_Returns404() throws Exception {
        // given
        given(userQueryUseCase.getMyProfile(1L))
                .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotFound());
    }

    @Test
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
