package com.example.GooRoomBe.account.domain.repository;

import com.example.GooRoomBe.account.domain.model.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findValidToken(String tokenValue, LocalDateTime now);
    void deleteByTokenValue(String tokenValue);
    void deleteAllByUserId(Long userId);
    RefreshToken save(RefreshToken refreshToken);
}