package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class DeviceTokenJpaRepositoryTest {

    @Autowired
    private DeviceTokenJpaRepository repository;

    @Test
    @DisplayName("existsByFcmToken: 존재하는 토큰이면 true를 반환한다")
    void existsByFcmToken_존재하는_토큰이면_true() {
        // given
        repository.save(new DeviceToken(1L, "token-1"));

        // when
        boolean result = repository.existsByFcmToken("token-1");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByFcmToken: 존재하지 않는 토큰이면 false를 반환한다")
    void existsByFcmToken_존재하지_않는_토큰이면_false() {
        // when
        boolean result = repository.existsByFcmToken("unknown-token");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("deleteByFcmToken: 해당 토큰의 row만 삭제한다")
    void deleteByFcmToken_해당_토큰만_삭제한다() {
        // given
        repository.save(new DeviceToken(1L, "token-1"));
        repository.save(new DeviceToken(2L, "token-2"));

        // when
        repository.deleteByFcmToken("token-1");

        // then
        assertThat(repository.existsByFcmToken("token-1")).isFalse();
        assertThat(repository.existsByFcmToken("token-2")).isTrue();
    }

    @Test
    @DisplayName("findAllFcmTokensByUserIdIn: 해당 유저들의 토큰을 모두 조회한다")
    void findAllFcmTokensByUserIdIn_해당_유저_토큰_조회() {
        // given
        repository.save(new DeviceToken(1L, "token-A"));
        repository.save(new DeviceToken(1L, "token-B"));
        repository.save(new DeviceToken(2L, "token-C"));
        repository.save(new DeviceToken(3L, "token-D"));

        // when
        List<String> tokens = repository.findAllFcmTokensByUserIdIn(List.of(1L, 2L));

        // then
        assertThat(tokens).hasSize(3);
        assertThat(tokens).containsExactlyInAnyOrder("token-A", "token-B", "token-C");
    }

    @Test
    @DisplayName("deleteAllByFcmTokenIn: 해당 토큰들을 일괄 삭제한다")
    void deleteAllByFcmTokenIn_일괄_삭제() {
        // given
        repository.save(new DeviceToken(1L, "dead-1"));
        repository.save(new DeviceToken(2L, "dead-2"));
        repository.save(new DeviceToken(3L, "alive"));

        // when
        repository.deleteAllByFcmTokenIn(List.of("dead-1", "dead-2"));

        // then
        assertThat(repository.existsByFcmToken("dead-1")).isFalse();
        assertThat(repository.existsByFcmToken("dead-2")).isFalse();
        assertThat(repository.existsByFcmToken("alive")).isTrue();
    }
}
