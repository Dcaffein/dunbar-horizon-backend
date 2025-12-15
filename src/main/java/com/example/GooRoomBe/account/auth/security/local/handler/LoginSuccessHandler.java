package com.example.GooRoomBe.account.auth.security.local.handler;

import com.example.GooRoomBe.account.auth.domain.token.RefreshToken;
import com.example.GooRoomBe.account.auth.security.core.cookie.AuthCookieManager;
import com.example.GooRoomBe.account.auth.security.core.jwt.JwtTokenProvider;
import com.example.GooRoomBe.account.auth.repository.RefreshTokenRepository;
import com.example.GooRoomBe.account.auth.security.local.LocalUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthCookieManager authCookieManager;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {

        LocalUserDetails userDetails = (LocalUserDetails) authentication.getPrincipal();
        String userId = String.valueOf(userDetails.getUser().getId());

        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(userId);

        RefreshToken newRefreshToken = new RefreshToken(userDetails.getUser(), refreshTokenValue);
        refreshTokenRepository.save(newRefreshToken);

        CsrfToken csrfTokenObj = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String csrfTokenValue = (csrfTokenObj != null) ? csrfTokenObj.getToken() : null;

        authCookieManager.addAuthCookies(response, accessToken, refreshTokenValue, csrfTokenValue);

        response.setStatus(HttpServletResponse.SC_OK);
    }
}