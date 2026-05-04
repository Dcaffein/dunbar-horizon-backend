package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class NotificationSettingJpaRepositoryTest {

    @Autowired
    private NotificationSettingJpaRepository repository;

    @Test
    @DisplayName("findAllByUserIdIn: 여러 사용자의 알림 설정 정보를 한 번에 조회한다")
    void findAllByUserIdIn_Success() {
        // given
        repository.save(new NotificationSetting(1L, "token-1"));
        repository.save(new NotificationSetting(2L, "token-2"));
        repository.save(new NotificationSetting(3L, "token-3"));

        // when
        List<NotificationSetting> settings = repository.findAllByUserIdIn(List.of(1L, 3L));

        // then
        assertThat(settings).hasSize(2);
        assertThat(settings).extracting("userId")
                .containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("findAllByFcmTokenIn: 죽은 토큰 목록으로 설정 정보를 정확히 조회한다")
    void findAllByFcmTokenIn_Success() {
        // given
        repository.save(new NotificationSetting(1L, "dead-token-1"));
        repository.save(new NotificationSetting(2L, "alive-token"));
        repository.save(new NotificationSetting(3L, "dead-token-2"));

        // when
        List<NotificationSetting> settings = repository.findAllByFcmTokenIn(List.of("dead-token-1", "dead-token-2"));

        // then
        assertThat(settings).hasSize(2);
        assertThat(settings).extracting("fcmToken")
                .containsExactlyInAnyOrder("dead-token-1", "dead-token-2");
    }
}