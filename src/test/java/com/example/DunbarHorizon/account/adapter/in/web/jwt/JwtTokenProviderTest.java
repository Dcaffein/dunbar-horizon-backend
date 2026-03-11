package com.example.DunbarHorizon.account.adapter.in.web.jwt;

import com.example.DunbarHorizon.global.security.JwtTokenProvider;
import com.example.DunbarHorizon.global.security.AuthPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "thisIsAVeryLongAndSecureSecretKeyForJWTAuthUsingHS512Algorithm1234567890abcdefghijklmn";

    @BeforeEach
    void setUp() {
        // 3600초 (1시간) 설정
        jwtTokenProvider = new JwtTokenProvider(secret, 3600, 7200);
    }

    @Test
    @DisplayName("UserPrincipal 정보를 바탕으로 유효한 토큰을 생성한다")
    void createToken_Success() {
        // given
        AuthPrincipal principal = new AuthPrincipal (1L, "ROLE_USER");

        // when
        String token = jwtTokenProvider.createAccessToken(principal);

        // then
        assertThat(token).isNotBlank();
        AuthPrincipal validated = jwtTokenProvider.validateToken(token);
        assertThat(validated.id()).isEqualTo(1L);
        assertThat(validated.role()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("만료된 토큰을 검증하면 ExpiredJwtException이 발생한다")
    void validateToken_Expired() {
        // 만료 시간이 0인 프로바이더 생성
        JwtTokenProvider expiredProvider = new JwtTokenProvider(secret, 0, 0);
        String token = expiredProvider.createAccessToken(new AuthPrincipal(1L, "ROLE_USER"));

        // when & then
        assertThatThrownBy(() -> expiredProvider.validateToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}