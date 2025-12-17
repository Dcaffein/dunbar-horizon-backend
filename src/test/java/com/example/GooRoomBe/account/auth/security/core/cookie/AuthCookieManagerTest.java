package com.example.GooRoomBe.account.auth.security.core.cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieManagerTest {

    private AuthCookieManager authCookieManager;

    @BeforeEach
    void setUp() {
        authCookieManager = new AuthCookieManager();
        ReflectionTestUtils.setField(authCookieManager, "accessExpiration", 3600L);
        ReflectionTestUtils.setField(authCookieManager, "refreshExpiration", 86400L);
    }

    @Test
    @DisplayName("인증 쿠키 추가: Access, Refresh")
    void addAuthCookies_Success() {
        // Given
        MockHttpServletResponse response = new MockHttpServletResponse();
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        // When
        authCookieManager.addAuthCookies(response, accessToken, refreshToken);

        // Then
        String allCookies = response.getHeaders("Set-Cookie").toString();

        assertThat(allCookies).contains("accessToken=access-token");
        assertThat(allCookies).contains("refreshToken=refresh-token");

        // Lax, HttpOnly 속성 확인
        assertThat(response.getHeaders("Set-Cookie").get(0)).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("인증 쿠키 삭제: 모든 쿠키의 Max-Age가 0이어야 한다")
    void clearAuthCookies_Success() {
        // Given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        authCookieManager.clearAuthCookies(response);

        // Then
        for (String cookieHeader : response.getHeaders("Set-Cookie")) {
            assertThat(cookieHeader).contains("Max-Age=0");
        }
    }
}