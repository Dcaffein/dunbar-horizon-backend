package com.example.GooRoomBe.account.auth.api;

import com.example.GooRoomBe.account.auth.api.dto.VerificationEmailSendRequestDto;
import com.example.GooRoomBe.account.auth.application.AuthTokenService;
import com.example.GooRoomBe.account.auth.application.LocalAccountService;
import com.example.GooRoomBe.account.auth.security.core.cookie.AuthCookieManager;
import com.example.GooRoomBe.support.ControllerTestSupport;
import com.example.GooRoomBe.support.WithCustomMockUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends ControllerTestSupport {

    @MockitoBean private LocalAccountService localAccountService;
    @MockitoBean private AuthTokenService authTokenService;
    @MockitoBean private AuthCookieManager authCookieManager;

    @Test
    @DisplayName("이메일 인증 요청: 성공 시 200 OK를 반환한다")
    void sendVerificationEmail_Success() throws Exception {
        VerificationEmailSendRequestDto requestDto =
                new VerificationEmailSendRequestDto("test@email.com", "/home");

        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(localAccountService).sendVerificationEmail("test@email.com", "/home");
    }

    @Test
    @DisplayName("이메일 인증 확인: 성공 시 200 OK를 반환한다")
    void verifyEmail_Success() throws Exception {
        String token = "valid-token-uuid";

        mockMvc.perform(get("/api/v1/auth/email-verifications")
                        .queryParam("token", token))
                .andDo(print())
                .andExpect(status().isOk());

        verify(localAccountService).verifyEmail(token);
    }

    @Test
    @DisplayName("토큰 재발급: RefreshToken 쿠키가 있으면 새 토큰을 발급한다")
    void reissueTokens_Success() throws Exception {
        String refreshToken = "valid-refresh-token";
        Cookie cookie = new Cookie("refreshToken", refreshToken);

        Map<String, String> mockTokens = Map.of(
                "accessToken", "new-access-token",
                "refreshToken", "valid-refresh-token"
        );
        given(authTokenService.reissueTokens(refreshToken)).willReturn(mockTokens);

        mockMvc.perform(post("/api/v1/auth/tokens")
                        .cookie(cookie))
                .andDo(print())
                .andExpect(status().isOk());

        verify(authTokenService).reissueTokens(refreshToken);
        verify(authCookieManager).addAuthCookies(any(HttpServletResponse.class), eq("new-access-token"), eq("valid-refresh-token"));
    }

    @Test
    @DisplayName("토큰 재발급: RefreshToken 쿠키가 없으면 400 Bad Request를 반환한다")
    void reissueTokens_Fail_NoCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/tokens"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그아웃: 성공 시 쿠키 삭제 매니저가 호출되어야 한다")
    @WithCustomMockUser
    void logout_Success() throws Exception {
        String refreshToken = "valid-refresh-token";
        Cookie cookie = new Cookie("refreshToken", refreshToken);

        mockMvc.perform(delete("/api/v1/auth/tokens")
                        .cookie(cookie))
                .andDo(print())
                .andExpect(status().isOk());

        verify(authTokenService).logout(refreshToken);
        verify(authCookieManager).clearAuthCookies(any(HttpServletResponse.class));
    }
}