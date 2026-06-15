package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.notification.adapter.out.persistence.jpa.DeviceTokenJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenRepositoryAdapterTest {

    @InjectMocks
    private DeviceTokenRepositoryAdapter adapter;

    @Mock
    private DeviceTokenJpaRepository jpaRepository;

    @Test
    @DisplayName("deleteAllByFcmTokenIn: 정상 토큰 리스트가 주어지면 JPA 레포지토리를 호출한다")
    void deleteAllByFcmTokenIn_정상_리스트면_JPA_호출() {
        // given
        List<String> tokens = List.of("token-1", "token-2");

        // when
        adapter.deleteAllByFcmTokenIn(tokens);

        // then
        verify(jpaRepository).deleteAllByFcmTokenIn(tokens);
    }

    @Test
    @DisplayName("deleteAllByFcmTokenIn: 빈 리스트가 주어지면 DB 쿼리 없이 종료한다")
    void deleteAllByFcmTokenIn_빈_리스트면_JPA_호출_안함() {
        // when
        adapter.deleteAllByFcmTokenIn(Collections.emptyList());

        // then
        verifyNoInteractions(jpaRepository);
    }

    @Test
    @DisplayName("deleteAllByFcmTokenIn: null이 주어지면 DB 쿼리 없이 종료한다")
    void deleteAllByFcmTokenIn_null이면_JPA_호출_안함() {
        // when
        adapter.deleteAllByFcmTokenIn(null);

        // then
        verifyNoInteractions(jpaRepository);
    }
}
