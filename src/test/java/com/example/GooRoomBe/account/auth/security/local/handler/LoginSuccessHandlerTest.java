package com.example.GooRoomBe.account.auth.security.local.handler;

import com.example.GooRoomBe.account.auth.domain.LocalAuth;
import com.example.GooRoomBe.account.auth.repository.RefreshTokenRepository;
import com.example.GooRoomBe.account.auth.security.core.cookie.AuthCookieManager;
import com.example.GooRoomBe.account.auth.security.core.jwt.JwtTokenProvider;
import com.example.GooRoomBe.account.auth.security.local.LocalUserDetails;
import com.example.GooRoomBe.account.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginSuccessHandlerTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock AuthCookieManager authCookieManager;

    @InjectMocks
    LoginSuccessHandler loginSuccessHandler;

    @Test
    @DisplayName("로그인 성공: 토큰 생성, 저장, 쿠키 설정이 순차적으로 실행되어야 한다")
    void onAuthenticationSuccess() throws IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn("1");

        LocalUserDetails userDetails = new LocalUserDetails(new LocalAuth(mockUser, "encodedPw"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null);

        given(jwtTokenProvider.createAccessToken("1")).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken("1")).willReturn("refresh-token");

        // When
        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // Then
        verify(refreshTokenRepository).save(any());
        verify(authCookieManager).addAuthCookies(
                any(HttpServletResponse.class),
                eq("access-token"),
                eq("refresh-token")
        );
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }
}