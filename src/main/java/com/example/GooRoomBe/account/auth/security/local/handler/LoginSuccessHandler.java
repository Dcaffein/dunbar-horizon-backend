package com.example.GooRoomBe.account.auth.security.local.handler;

import com.example.GooRoomBe.account.auth.domain.token.RefreshToken;
import com.example.GooRoomBe.account.auth.security.core.cookie.AuthCookieManager;
import com.example.GooRoomBe.account.auth.security.core.jwt.JwtTokenProvider;
import com.example.GooRoomBe.account.auth.repository.RefreshTokenRepository;
import com.example.GooRoomBe.account.auth.security.local.LocalUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthCookieManager authCookieManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        LocalUserDetails userDetails = (LocalUserDetails) authentication.getPrincipal();
        String userId = String.valueOf(userDetails.getUser().getId());

        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(userId);

        RefreshToken newRefreshToken = new RefreshToken(userDetails.getUser(), refreshTokenValue);
        refreshTokenRepository.save(newRefreshToken);

        authCookieManager.addAuthCookies(response, accessToken, refreshTokenValue);

        response.setStatus(HttpStatus.OK.value());
    }
}