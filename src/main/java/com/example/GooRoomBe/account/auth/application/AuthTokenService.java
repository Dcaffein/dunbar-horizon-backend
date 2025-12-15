package com.example.GooRoomBe.account.auth.application;

import com.example.GooRoomBe.account.auth.exception.RefreshTokenNotFoundException;
import com.example.GooRoomBe.account.auth.repository.RefreshTokenRepository;
import com.example.GooRoomBe.account.auth.security.core.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void logout(String refreshTokenValue) {
        jwtTokenProvider.validateToken(refreshTokenValue);
        String userId = jwtTokenProvider.getUserIdFromToken(refreshTokenValue);
        refreshTokenRepository.deleteByUser_IdAndTokenValue(userId, refreshTokenValue);
    }

    @Transactional
    public Map<String, String> reissueTokens(String refreshTokenValue) {
        jwtTokenProvider.validateToken(refreshTokenValue);

        String userId = jwtTokenProvider.getUserIdFromToken(refreshTokenValue);

        if (!refreshTokenRepository.existsByUser_IdAndTokenValue(userId, refreshTokenValue)) {
            throw new RefreshTokenNotFoundException();
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);

        return Map.of("accessToken", newAccessToken, "refreshToken", refreshTokenValue);
    }
}
