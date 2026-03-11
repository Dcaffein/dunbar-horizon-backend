package com.example.DunbarHorizon.account.application.port.out;

import com.example.DunbarHorizon.global.security.AuthPrincipal;

import java.time.LocalDateTime;

public interface AuthTokenProvider {
    String createAccessToken(AuthPrincipal principal);
    String createRefreshToken(AuthPrincipal principal);
    AuthPrincipal validateToken(String token);
    LocalDateTime getExpirationTime(String token);
}
