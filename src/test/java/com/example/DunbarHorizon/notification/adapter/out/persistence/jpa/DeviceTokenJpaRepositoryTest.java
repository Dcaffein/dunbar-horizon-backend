package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class DeviceTokenJpaRepositoryTest {

    @Autowired
    private DeviceTokenJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("deleteByToken: 해당 토큰의 row만 삭제한다")
    void deleteByToken_해당_토큰만_삭제한다() {
        // given
        repository.save(new DeviceToken(1L, "token-1"));
        repository.save(new DeviceToken(2L, "token-2"));

        // when
        repository.deleteByToken("token-1");

        // then
        assertThat(repository.existsById("token-1")).isFalse();
        assertThat(repository.existsById("token-2")).isTrue();
    }

    @Test
    @DisplayName("deleteByUserIdAndToken: 사용자와 토큰이 모두 맞을 때만 지우고 지운 행 수를 돌려준다")
    void deleteByUserIdAndToken_주인이_맞을_때만_삭제한다() {
        // given
        repository.save(new DeviceToken(1L, "token-mine"));
        repository.save(new DeviceToken(2L, "token-theirs"));

        // when
        int deletedMine = repository.deleteByUserIdAndToken(1L, "token-mine");
        int deletedTheirs = repository.deleteByUserIdAndToken(1L, "token-theirs");

        // then
        assertThat(deletedMine).isEqualTo(1);
        assertThat(deletedTheirs).isZero();
        assertThat(repository.existsById("token-mine")).isFalse();
        assertThat(repository.existsById("token-theirs")).isTrue();
    }

    @Test
    @DisplayName("findAllTokensByUserIdIn: 해당 유저들의 토큰을 모두 조회한다")
    void findAllTokensByUserIdIn_해당_유저_토큰_조회() {
        // given
        repository.save(new DeviceToken(1L, "token-A"));
        repository.save(new DeviceToken(1L, "token-B"));
        repository.save(new DeviceToken(2L, "token-C"));
        repository.save(new DeviceToken(3L, "token-D"));

        // when
        List<String> tokens = repository.findAllTokensByUserIdIn(List.of(1L, 2L));

        // then
        assertThat(tokens).hasSize(3);
        assertThat(tokens).containsExactlyInAnyOrder("token-A", "token-B", "token-C");
    }

    @Test
    @DisplayName("deleteAllByTokenIn: 해당 토큰들을 일괄 삭제한다")
    void deleteAllByTokenIn_일괄_삭제() {
        // given
        repository.save(new DeviceToken(1L, "dead-1"));
        repository.save(new DeviceToken(2L, "dead-2"));
        repository.save(new DeviceToken(3L, "alive"));

        // when
        repository.deleteAllByTokenIn(List.of("dead-1", "dead-2"));

        // then
        assertThat(repository.existsById("dead-1")).isFalse();
        assertThat(repository.existsById("dead-2")).isFalse();
        assertThat(repository.existsById("alive")).isTrue();
    }

    @Test
    @DisplayName("save: 다른 사용자가 같은 토큰을 저장하면 주인만 바뀌고 원래 사용자의 다른 토큰은 남는다")
    void save_같은_토큰이면_주인만_바뀐다() {
        // given
        repository.save(new DeviceToken(1L, "token-shared"));
        repository.save(new DeviceToken(1L, "token-other"));
        entityManager.flush();
        entityManager.clear();

        // when
        repository.save(new DeviceToken(2L, "token-shared"));
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(repository.findAllTokensByUserIdIn(List.of(1L))).containsExactly("token-other");
        assertThat(repository.findAllTokensByUserIdIn(List.of(2L))).containsExactly("token-shared");
        assertThat(repository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("save: 한 사용자가 토큰을 여러 개 저장하면 모두 남는다")
    void save_한_사용자의_여러_토큰이_모두_남는다() {
        // when
        repository.save(new DeviceToken(1L, "token-phone"));
        repository.save(new DeviceToken(1L, "token-pc"));
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(repository.findAllTokensByUserIdIn(List.of(1L)))
                .containsExactlyInAnyOrder("token-phone", "token-pc");
    }
}
