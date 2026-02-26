package com.example.GooRoomBe.account.adapter.in.web.OAuth2;

import com.example.GooRoomBe.account.adapter.in.web.cookie.AuthCookieManager;
import com.example.GooRoomBe.account.application.port.in.LoginUseCase;
import com.example.GooRoomBe.account.application.port.in.dto.AuthTokenResult;
import com.example.GooRoomBe.account.domain.model.User;
import com.example.GooRoomBe.account.domain.model.UserRole;
import jakarta.servlet.ServletException;
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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @Mock
    private LoginUseCase loginUseCase;

    @Mock
    private AuthCookieManager authCookieManager;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendBaseUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("소셜 로그인 성공 시 유즈케이스를 통해 토큰을 발행하고 쿠키를 굽고 리다이렉트한다")
    void onAuthenticationSuccess_RedirectsToFrontend() throws IOException, ServletException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        CustomOAuth2User userDetails = new CustomOAuth2User(user, java.util.Collections.emptyMap());
        given(authentication.getPrincipal()).willReturn(userDetails);

        AuthTokenResult tokenResult = new AuthTokenResult("at", "rt");
        given(loginUseCase.issueTokens(user)).willReturn(tokenResult);

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(loginUseCase).issueTokens(user);

        //  쿠키 매니저 호출 검증
        verify(authCookieManager).addAccessTokenCookie(eq(response), eq("at"));
        verify(authCookieManager).addRefreshTokenCookie(eq(response), eq("rt"));

        //  리다이렉트 URL 검증
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000");
    }
}