package com.example.DunbarHorizon.account.adapter.in.web;

import com.example.DunbarHorizon.account.application.dto.MyProfileResult;
import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.adapter.in.web.dto.UserProfileUpdateRequest;
import com.example.DunbarHorizon.account.domain.exception.UserNotFoundException;
import com.example.DunbarHorizon.global.imageStorage.PresignedUploadResult;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("profileImageKey와 함께 프로필을 수정하면 200 OK를 반환하고 updateProfile()을 호출한다")
    void updateProfile_withImageKey_Success() throws Exception {
        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("새닉네임", "profiles/uuid-photo");

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userProfileUpdateUseCase).updateProfile(any(), eq("새닉네임"), eq("profiles/uuid-photo"));
    }

    @Test
    @DisplayName("profileImageKey 없이 닉네임만 수정하면 200 OK를 반환하고 profileImageKey=null로 updateProfile()을 호출한다")
    void updateProfile_withoutImageKey_Success() throws Exception {
        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("새닉네임", null);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userProfileUpdateUseCase).updateProfile(any(), eq("새닉네임"), isNull());
    }

    @Test
    @DisplayName("프로필 이미지 presign 요청 시 생성 결과를 반환한다")
    void presignProfileImage_Success() throws Exception {
        // given
        PresignedUploadResult result = new PresignedUploadResult("https://upload.example.com", "profiles/uuid-photo");
        given(profileImageStoragePort.presignUpload("image/png")).willReturn(result);

        // when & then
        mockMvc.perform(post("/api/v1/users/me/profile-image/presign")
                        .param("contentType", "image/png"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("https://upload.example.com"))
                .andExpect(jsonPath("$.objectKey").value("profiles/uuid-photo"));

        verify(profileImageStoragePort).presignUpload("image/png");
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
