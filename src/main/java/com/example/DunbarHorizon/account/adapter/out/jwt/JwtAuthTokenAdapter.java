package com.example.DunbarHorizon.account.adapter.out.jwt;

import com.example.DunbarHorizon.account.application.port.out.AuthTokenProvider;
import com.example.DunbarHorizon.global.security.AuthPrincipal;
import com.example.DunbarHorizon.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAuthTokenAdapter implements AuthTokenProvider {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public String createAccessToken(AuthPrincipal principal) {
        return jwtTokenProvider.createAccessToken(principal);
    }

    @Override
    public String createRefreshToken(AuthPrincipal principal) {
        return jwtTokenProvider.createRefreshToken(principal);
    }

    @Override
    public AuthPrincipal validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public LocalDateTime getExpirationTime(String token) {
        return jwtTokenProvider.getExpirationTime(token);
    }
}
