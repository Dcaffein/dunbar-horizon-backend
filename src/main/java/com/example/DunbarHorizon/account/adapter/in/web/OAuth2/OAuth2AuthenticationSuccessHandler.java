package com.example.DunbarHorizon.account.adapter.in.web.OAuth2;

import com.example.DunbarHorizon.global.security.AuthCookieManager;
import com.example.DunbarHorizon.account.application.service.LoginService;
import com.example.DunbarHorizon.account.application.dto.AuthTokenResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final LoginService loginService;
    private final AuthCookieManager authCookieManager;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User userDetails = (CustomOAuth2User) authentication.getPrincipal();

        AuthTokenResult tokenResult = loginService.issueTokens(userDetails.getUser());

        authCookieManager.addAccessTokenCookie(response, tokenResult.accessToken());
        authCookieManager.addRefreshTokenCookie(response, tokenResult.refreshToken());

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, frontendBaseUrl);
    }
}