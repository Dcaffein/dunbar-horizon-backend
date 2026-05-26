package com.example.DunbarHorizon.account.adapter.in.web;

import com.example.DunbarHorizon.account.adapter.in.web.dto.LoginRequestDto;
import com.example.DunbarHorizon.account.adapter.in.web.dto.SignupRequestDto;
import com.example.DunbarHorizon.account.adapter.in.web.dto.UserProfileUpdateRequest;
import com.example.DunbarHorizon.account.adapter.in.web.dto.VerificationEmailRequestDto;
import com.example.DunbarHorizon.account.application.dto.AuthTokenResult;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerTest extends BaseControllerTest {

    @Test
    @DisplayName("회원가입 요청 시 201 Created를 반환한다")
    void signup_Success() throws Exception {
        SignupRequestDto request = new SignupRequestDto("test@test.com", "tester", "Pw123!@#");

        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(signupUseCase).signup(eq("test@test.com"), eq("Pw123!@#"), eq("tester"));
    }

    @Test
    @DisplayName("로그인 성공 시 쿠키를 설정하고 201 Created를 반환한다")
    void login_Success() throws Exception {
        LoginRequestDto request = new LoginRequestDto("test@test.com", "password123");
        AuthTokenResult result = new AuthTokenResult("access-token", "refresh-token");

        given(loginUseCase.login(anyString(), anyString())).willReturn(result);

        mockMvc.perform(post("/api/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authCookieManager).addAccessTokenCookie(any(), eq("access-token"));
        verify(authCookieManager).addRefreshTokenCookie(any(), eq("refresh-token"));
    }

    @Test
    @DisplayName("로그아웃 시 쿠키를 만료시키고 204 No Content를 반환한다")
    void logout_Success() throws Exception {
        mockMvc.perform(delete("/api/auth/tokens")
                        .cookie(new Cookie("refresh_token", "some-rt")))
                .andExpect(status().isNoContent());

        verify(loginUseCase).logout(eq("some-rt"));
        verify(authCookieManager).addExpiredTokenCookie(any());
    }

    @Test
    @DisplayName("토큰 재발급 시 새로운 쿠키를 설정하고 200 OK를 반환한다")
    void reissue_Success() throws Exception {
        String oldRt = "old-rt";
        AuthTokenResult newResult = new AuthTokenResult("new-at", "new-rt");

        given(loginUseCase.reissue(oldRt)).willReturn(newResult);

        mockMvc.perform(patch("/api/auth/tokens")
                        .cookie(new Cookie("refresh_token", oldRt)))
                .andExpect(status().isOk());

        verify(authCookieManager).addAccessTokenCookie(any(), eq("new-at"));
        verify(authCookieManager).addRefreshTokenCookie(any(), eq("new-rt"));
    }

    @Test
    @DisplayName("이메일 인증 메일 발송 요청 시 201 Created를 반환한다")
    void sendVerificationEmail_Success() throws Exception {
        VerificationEmailRequestDto request = new VerificationEmailRequestDto("test@test.com", "http://redirect.com");

        mockMvc.perform(post("/api/auth/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(verificationUseCase).sendVerificationEmail(eq("test@test.com"), eq("http://redirect.com"));
    }

    @Test
    @DisplayName("이메일 토큰 검증 성공 시 200 OK를 반환한다")
    void verifyEmail_Success() throws Exception {
        String token = "valid-token";

        mockMvc.perform(patch("/api/auth/verifications")
                        .param("token", token))
                .andExpect(status().isOk());

        verify(verificationUseCase).verifyEmail(eq(token));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("profileImageKey와 함께 프로필을 수정하면 200 OK를 반환하고 updateProfile()을 호출한다")
    void updateProfile_withImageKey_Success() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("새닉네임", "profiles/uuid-photo");

        mockMvc.perform(patch("/api/auth/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userProfileUpdateUseCase).updateProfile(any(), eq("새닉네임"), eq("profiles/uuid-photo"));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("profileImageKey 없이 닉네임만 수정하면 200 OK를 반환하고 profileImageKey=null로 updateProfile()을 호출한다")
    void updateProfile_withoutImageKey_Success() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("새닉네임", null);

        mockMvc.perform(patch("/api/auth/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userProfileUpdateUseCase).updateProfile(any(), eq("새닉네임"), isNull());
    }
}
