package com.example.GooRoomBe.account.application.port.out;

import com.example.GooRoomBe.account.application.port.in.dto.AuthPrincipal;

public interface AuthTokenProvider {
    String createAccessToken(AuthPrincipal principal);
    String createRefreshToken(AuthPrincipal principal);
    AuthPrincipal validateToken(String token);
}
