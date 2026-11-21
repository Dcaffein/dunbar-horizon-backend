package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import com.example.DunbarHorizon.notification.domain.repository.DeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    @DisplayName("registerDeviceToken: 토큰을 사용자의 기기로 저장한다")
    void registerDeviceToken_토큰을_사용자_기기로_저장한다() {
        // when
        deviceTokenService.registerDeviceToken(1L, "new-token");

        // then
        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getToken()).isEqualTo("new-token");
    }

    @Test
    @DisplayName("deregisterDeviceToken: 사용자와 토큰 조건으로 삭제한다")
    void deregisterDeviceToken_사용자와_토큰_조건으로_삭제한다() {
        // when
        deviceTokenService.deregisterDeviceToken(1L, "my-token");

        // then
        verify(deviceTokenRepository).deleteByUserIdAndToken(1L, "my-token");
    }

    @Test
    @DisplayName("isTokenRegisteredForUser: userId와 토큰이 모두 일치하면 true를 반환한다")
    void isTokenRegisteredForUser_일치하면_true() {
        // given
        given(deviceTokenRepository.existsByUserIdAndToken(1L, "registered-token")).willReturn(true);

        // when
        boolean result = deviceTokenService.isTokenRegisteredForUser(1L, "registered-token");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isTokenRegisteredForUser: 토큰이 없거나 userId가 다르면 false를 반환한다")
    void isTokenRegisteredForUser_불일치하면_false() {
        // given
        given(deviceTokenRepository.existsByUserIdAndToken(1L, "unknown-token")).willReturn(false);

        // when
        boolean result = deviceTokenService.isTokenRegisteredForUser(1L, "unknown-token");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("removeDeviceToken: 주어진 토큰을 삭제한다")
    void removeDeviceToken_토큰을_삭제한다() {
        // when
        deviceTokenService.removeDeviceToken("token-to-remove");

        // then
        verify(deviceTokenRepository).deleteByToken("token-to-remove");
    }

    @Test
    @DisplayName("cleanupInvalidTokens: 유효하지 않은 토큰 목록을 일괄 삭제한다")
    void cleanupInvalidTokens_유효하지않은_토큰들을_일괄_삭제한다() {
        // given
        List<String> invalidTokens = List.of("dead-token-1", "dead-token-2");

        // when
        deviceTokenService.cleanupInvalidTokens(invalidTokens);

        // then
        verify(deviceTokenRepository).deleteAllByTokenIn(invalidTokens);
    }

    @Test
    @DisplayName("cleanupInvalidTokens: 빈 리스트면 삭제 호출을 하지 않는다")
    void cleanupInvalidTokens_빈_리스트면_삭제_호출_안함() {
        // when
        deviceTokenService.cleanupInvalidTokens(List.of());

        // then
        verify(deviceTokenRepository, never()).deleteAllByTokenIn(any());
    }

    @Test
    @DisplayName("findTokensByUserIds: 사용자들의 기기 토큰을 조회한다")
    void findTokensByUserIds_사용자들의_토큰을_조회한다() {
        // given
        given(deviceTokenRepository.findAllTokensByUserIdIn(List.of(1L, 2L)))
                .willReturn(List.of("token-1", "token-2"));

        // when
        List<String> tokens = deviceTokenService.findTokensByUserIds(List.of(1L, 2L));

        // then
        assertThat(tokens).containsExactly("token-1", "token-2");
    }
}
