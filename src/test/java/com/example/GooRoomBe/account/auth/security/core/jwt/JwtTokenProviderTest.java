package com.example.GooRoomBe.account.auth.security.core.jwt;

import com.example.GooRoomBe.account.auth.exception.ExpiredTokenException;
import com.example.GooRoomBe.account.auth.exception.InvalidJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "c2VjcmV0LWtleS10ZXN0LXNlY3JldC1rZXktdGVzdC1secrldC1rZXk=";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessExpirationSeconds", 3600L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationSeconds", 86400L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("토큰 생성 및 검증: 유효한 토큰이면 통과한다")
    void createAndValidateToken_Success() {
        // Given
        String userId = "123";

        // When
        String accessToken = jwtTokenProvider.createAccessToken(userId);

        // Then
        assertDoesNotThrow(() -> jwtTokenProvider.validateToken(accessToken));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(accessToken));
    }

    @Test
    @DisplayName("토큰 검증: 잘못된 토큰이면 예외가 발생한다")
    void validateToken_Fail_Invalid() {
        // Given
        String invalidToken = "invalid.token.value";

        // When & Then
        assertThrows(InvalidJwtException.class, () -> jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    @DisplayName("토큰 검증: 만료된 토큰이면 ExpiredTokenException이 발생한다")
    void validateToken_Fail_Expired() {
        // Given: 생성 즉시 만료됨
        ReflectionTestUtils.setField(jwtTokenProvider, "accessExpirationSeconds", 0L);

        String userId = "123";
        String expiredToken = jwtTokenProvider.createAccessToken(userId);

        // When & Then
        assertThrows(ExpiredTokenException.class, () -> jwtTokenProvider.validateToken(expiredToken));
    }
}