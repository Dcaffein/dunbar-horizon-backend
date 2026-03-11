package com.example.DunbarHorizon.account.domain.repository;

import com.example.DunbarHorizon.account.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenValue(String tokenValue);
    void deleteByTokenValue(String tokenValue);
    void deleteAllByUserId(Long userId);
    RefreshToken save(RefreshToken refreshToken);
}