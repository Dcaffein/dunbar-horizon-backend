package com.example.DunbarHorizon.account.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.RefreshTokenJpaRepository;
import com.example.DunbarHorizon.account.domain.model.RefreshToken;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class RefreshTokenJpaRepositoryTest {

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Autowired
    private TestEntityManager em;



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
        assertThat(refreshTokenJpaRepository.findByTokenValue("tk1")).isEmpty();
        assertThat(refreshTokenJpaRepository.findByTokenValue("tk2")).isEmpty();
    }
}