package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.notification.adapter.out.persistence.jpa.NotificationSettingJpaRepository;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationSettingRepositoryAdapterTest {

    @InjectMocks
    private NotificationSettingRepositoryAdapter adapter;

    @Mock
    private NotificationSettingJpaRepository jpaRepository;

    @Test
    @DisplayName("findAllByFcmTokenIn: 정상적인 토큰 리스트가 주어지면 JPA 레포지토리를 호출한다")
    void findAllByFcmTokenIn_CallsJpaRepository() {
        // given
        List<String> tokens = List.of("token1", "token2");
        NotificationSetting setting = new NotificationSetting(1L, "token1");
        given(jpaRepository.findAllByFcmTokenIn(tokens)).willReturn(List.of(setting));

        // when
        List<NotificationSetting> result = adapter.findAllByFcmTokenIn(tokens);

        // then
        assertThat(result).hasSize(1);
        verify(jpaRepository).findAllByFcmTokenIn(tokens);
    }

    @Test
    @DisplayName("findAllByFcmTokenIn: 빈 리스트나 null이 주어지면 DB 쿼리 없이 빈 리스트를 반환한다")
    void findAllByFcmTokenIn_EmptyOrNull_ReturnsEmptyList() {
        // when (null 인 경우)
        List<NotificationSetting> nullResult = adapter.findAllByFcmTokenIn(null);
        // when (빈 리스트인 경우)
        List<NotificationSetting> emptyResult = adapter.findAllByFcmTokenIn(Collections.emptyList());

        // then
        assertThat(nullResult).isEmpty();
        assertThat(emptyResult).isEmpty();

        // 실제 DB 쿼리가 단 한 번도 호출되지 않았음을 보장
        verifyNoInteractions(jpaRepository);
    }
}