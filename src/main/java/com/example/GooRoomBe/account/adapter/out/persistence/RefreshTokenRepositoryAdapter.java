package com.example.GooRoomBe.account.adapter.out.persistence;

import com.example.GooRoomBe.account.adapter.out.persistence.jpa.RefreshTokenJpaRepository;
import com.example.GooRoomBe.account.domain.model.RefreshToken;
import com.example.GooRoomBe.account.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Override
    public Optional<RefreshToken> findValidToken(String tokenValue, LocalDateTime now) {
        return refreshTokenJpaRepository.findValidToken(tokenValue, now);
    }

    @Override
    public void deleteByTokenValue(String tokenValue) {
        refreshTokenJpaRepository.deleteByTokenValue(tokenValue);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        refreshTokenJpaRepository.deleteAllByUserId(userId);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return refreshTokenJpaRepository.save(refreshToken);
    }
}
