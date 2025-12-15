package com.example.GooRoomBe.account.auth.application;

import com.example.GooRoomBe.account.auth.exception.RefreshTokenNotFoundException;
import com.example.GooRoomBe.account.auth.repository.RefreshTokenRepository;
import com.example.GooRoomBe.account.auth.security.core.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthTokenService authTokenService;

    @Test
    @DisplayName("토큰 재발급: 유효한 RefreshToken이면 새 AccessToken을 반환한다")
    void reissueTokens_Success() {
        // Given
        String refreshToken = "valid-refresh-token";
        String userId = "user-1";
        String newAccessToken = "new-access-token";

        // 토큰 검증 통과 및 ID 추출
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        // DB에 해당 토큰 존재함
        given(refreshTokenRepository.existsByUser_IdAndTokenValue(userId, refreshToken)).willReturn(true);
        // 새 토큰 생성
        given(jwtTokenProvider.createAccessToken(userId)).willReturn(newAccessToken);

        // When
        Map<String, String> result = authTokenService.reissueTokens(refreshToken);

        // Then
        assertEquals(newAccessToken, result.get("accessToken"));
        assertEquals(refreshToken, result.get("refreshToken")); // 기존 토큰 유지 확인

        verify(jwtTokenProvider).validateToken(refreshToken); // 검증 메서드 호출 확인
    }

    @Test
    @DisplayName("토큰 재발급: DB에 없는 토큰이면 예외가 발생한다")
    void reissueTokens_Fail_NotFound() {
        // Given
        String refreshToken = "unknown-token";
        String userId = "user-1";

        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(refreshTokenRepository.existsByUser_IdAndTokenValue(userId, refreshToken)).willReturn(false);

        // When & Then
        assertThrows(RefreshTokenNotFoundException.class,
                () -> authTokenService.reissueTokens(refreshToken));
    }
}