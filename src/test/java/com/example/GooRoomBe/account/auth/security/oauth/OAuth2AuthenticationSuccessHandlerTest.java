package com.example.GooRoomBe.account.auth.security.oauth;

import com.example.GooRoomBe.account.auth.repository.RefreshTokenRepository;
import com.example.GooRoomBe.account.auth.security.core.cookie.AuthCookieManager;
import com.example.GooRoomBe.account.auth.security.core.jwt.JwtTokenProvider;
import com.example.GooRoomBe.account.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock AuthCookieManager authCookieManager;

    @InjectMocks OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "frontendUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("소셜 로그인 성공: 쿠키 설정 후 프론트엔드로 리다이렉트 해야 한다")
    void onAuthenticationSuccess() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        //  User 객체 Mocking
        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn("100");

        //  CustomOAuth2User Mocking
        CustomOAuth2User oAuth2User = mock(CustomOAuth2User.class);
        given(oAuth2User.getUser()).willReturn(mockUser);

        // Authentication Mocking
        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(oAuth2User);

        // (User ID "100"에 대해 토큰 반환)
        given(jwtTokenProvider.createAccessToken("100")).willReturn("access-100");
        given(jwtTokenProvider.createRefreshToken("100")).willReturn("refresh-100");

        // When
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Then
        verify(authCookieManager).addAuthCookies(
                any(HttpServletResponse.class),
                eq("access-100"),
                eq("refresh-100")
        );

        // 리다이렉트 확인
        assertEquals("http://localhost:3000/", response.getRedirectedUrl());
    }
}