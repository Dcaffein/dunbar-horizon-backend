package com.example.GooRoomBe.account.adapter.in.web.jwt;

import com.example.GooRoomBe.account.adapter.in.web.cookie.AuthCookieManager;
import com.example.GooRoomBe.account.adapter.out.jwt.JwtAuthenticationFilter;
import com.example.GooRoomBe.account.adapter.out.jwt.JwtTokenProvider;
import com.example.GooRoomBe.account.application.port.in.dto.AuthPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private JwtTokenProvider jwtProvider;
    @Mock
    private AuthCookieManager authCookieManager;
    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("유효한 쿠키 토큰이 있으면 시큐리티 컨텍스트에 인증 정보가 설정된다")
    void doFilterInternal_Success() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String token = "valid-token";
        AuthPrincipal principal = new AuthPrincipal(1L, "ROLE_USER");

        given(authCookieManager.extractAccessToken(request)).willReturn(token);
        given(jwtProvider.validateToken(token)).willReturn(principal);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((AuthPrincipal) auth.getPrincipal()).id()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }
}