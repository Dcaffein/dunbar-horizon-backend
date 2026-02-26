package com.example.GooRoomBe.account.adapter.out.persistence;

import com.example.GooRoomBe.account.adapter.out.persistence.jpa.RefreshTokenJpaRepository;
import com.example.GooRoomBe.account.domain.model.RefreshToken;
import com.example.GooRoomBe.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class RefreshTokenJpaRepositoryTest {

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("findValidToken: 토큰 값과 만료 시간이 현재 시간보다 이후인 토큰만 조회한다")
    void findValidToken_Success() {
        // given
        String validToken = "valid-token";
        String expiredToken = "expired-token";
        LocalDateTime now = LocalDateTime.now();

        // 1. 유효한 토큰 저장 (1시간 뒤 만료)
        RefreshToken active = RefreshToken.builder()
                .userId(1L)
                .tokenValue(validToken)
                .build();
        ReflectionTestUtils.setField(active, "expiryDate", now.plusHours(1));
        em.persist(active);

        // 2. 만료된 토큰 저장 (1시간 전 만료)
        RefreshToken expired = RefreshToken.builder()
                .userId(1L)
                .tokenValue(expiredToken)
                .build();
        ReflectionTestUtils.setField(expired, "expiryDate", now.minusHours(1));
        em.persist(expired);

        em.flush();
        em.clear();

        // when
        Optional<RefreshToken> resultValid = refreshTokenJpaRepository.findValidToken(validToken, now);
        Optional<RefreshToken> resultExpired = refreshTokenJpaRepository.findValidToken(expiredToken, now);

        // then
        assertThat(resultValid).isPresent();
        assertThat(resultValid.get().getTokenValue()).isEqualTo(validToken);

        // 만료된 토큰은 쿼리 조건(expiryDate > :now)에 의해 조회되지 않아야 함
        assertThat(resultExpired).isEmpty();
    }

    @Test
    @DisplayName("deleteAllByUserId: @Modifying 쿼리를 통해 특정 유저의 모든 토큰을 삭제한다")
    void deleteAllByUserId_Success() {
        // given
        Long userId = 10L;
        LocalDateTime future = LocalDateTime.now().plusDays(1);

        RefreshToken tk1 = RefreshToken.builder().userId(userId).tokenValue("tk1").build();
        RefreshToken tk2 = RefreshToken.builder().userId(userId).tokenValue("tk2").build();
        ReflectionTestUtils.setField(tk1, "expiryDate", future);
        ReflectionTestUtils.setField(tk2, "expiryDate", future);

        em.persist(tk1);
        em.persist(tk2);
        em.flush();

        // when
        refreshTokenJpaRepository.deleteAllByUserId(userId);

        // 벌크 연산 후에는 영속성 컨텍스트와 DB의 동기화를 위해 clear 필수
        em.clear();

        // then
        // 아주 오래된 시간을 기준으로 조회해도 토큰이 존재하지 않아야 함 (삭제 확인)
        LocalDateTime longAgo = LocalDateTime.now().minusYears(10);
        assertThat(refreshTokenJpaRepository.findValidToken("tk1", longAgo)).isEmpty();
        assertThat(refreshTokenJpaRepository.findValidToken("tk2", longAgo)).isEmpty();
    }
}