package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.event.DeviceTokenRegisteredEvent;
import com.example.DunbarHorizon.notification.domain.repository.DeviceTokenRepository;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("registerDeviceToken: 신규 토큰이면 기존 토큰을 삭제하고 저장 후 이벤트를 발행한다")
    void registerDeviceToken_신규_토큰이면_기존_삭제_후_저장하고_이벤트를_발행한다() {
        // given
        given(deviceTokenRepository.existsByUserIdAndFcmToken(1L, "new-token")).willReturn(false);

        // when
        notificationService.registerDeviceToken(1L, "new-token");

        // then
        verify(deviceTokenRepository).deleteAllByUserId(1L);

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getFcmToken()).isEqualTo("new-token");

        verify(eventPublisher).publishEvent(any(DeviceTokenRegisteredEvent.class));
    }

    @Test
    @DisplayName("registerDeviceToken: 동일 유저의 동일 토큰이면 no-op")
    void registerDeviceToken_동일_유저의_동일_토큰이면_무시한다() {
        // given
        given(deviceTokenRepository.existsByUserIdAndFcmToken(1L, "existing-token")).willReturn(true);

        // when
        notificationService.registerDeviceToken(1L, "existing-token");

        // then
        verify(deviceTokenRepository, never()).deleteAllByUserId(any());
        verify(deviceTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("removeDeviceTokenByUserId: userId 기준으로 토큰을 삭제한다")
    void removeDeviceTokenByUserId_userId_기준으로_삭제한다() {
        // when
        notificationService.removeDeviceTokenByUserId(1L);

        // then
        verify(deviceTokenRepository).deleteAllByUserId(1L);
    }

    @Test
    @DisplayName("isTokenRegisteredForUser: userId와 토큰이 모두 일치하면 true를 반환한다")
    void isTokenRegisteredForUser_일치하면_true() {
        // given
        given(deviceTokenRepository.existsByUserIdAndFcmToken(1L, "registered-token")).willReturn(true);

        // when
        boolean result = notificationService.isTokenRegisteredForUser(1L, "registered-token");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isTokenRegisteredForUser: 토큰이 없거나 userId가 다르면 false를 반환한다")
    void isTokenRegisteredForUser_불일치하면_false() {
        // given
        given(deviceTokenRepository.existsByUserIdAndFcmToken(1L, "unknown-token")).willReturn(false);

        // when
        boolean result = notificationService.isTokenRegisteredForUser(1L, "unknown-token");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("removeDeviceToken: 주어진 토큰을 삭제한다")
    void removeDeviceToken_토큰을_삭제한다() {
        // when
        notificationService.removeDeviceToken("token-to-remove");

        // then
        verify(deviceTokenRepository).deleteByFcmToken("token-to-remove");
    }

    @Test
    @DisplayName("cleanupInvalidTokens: 유효하지 않은 토큰 목록을 일괄 삭제한다")
    void cleanupInvalidTokens_유효하지않은_토큰들을_일괄_삭제한다() {
        // given
        List<String> invalidTokens = List.of("dead-token-1", "dead-token-2");

        // when
        notificationService.cleanupInvalidTokens(invalidTokens);

        // then
        verify(deviceTokenRepository).deleteAllByFcmTokenIn(invalidTokens);
    }

    @Test
    @DisplayName("cleanupInvalidTokens: 빈 리스트면 삭제 호출을 하지 않는다")
    void cleanupInvalidTokens_빈_리스트면_삭제_호출_안함() {
        // when
        notificationService.cleanupInvalidTokens(List.of());

        // then
        verify(deviceTokenRepository, never()).deleteAllByFcmTokenIn(any());
    }

    @Test
    @DisplayName("markAsSent: 알림을 조회하여 발송 완료 상태로 변경한다")
    void markAsSent_발송_완료_상태로_변경한다() {
        // given
        Notification notice = Notification.builder().receiverId(1L).isSent(false).build();
        given(notificationRepository.findById("notice-id")).willReturn(Optional.of(notice));

        // when
        notificationService.markAsSent("notice-id");

        // then
        assertThat(notice.isSent()).isTrue();
        verify(notificationRepository).save(notice);
    }

    @Test
    @DisplayName("markAllAsSent: 다수의 알림을 일괄 발송 완료 상태로 변경한다")
    void markAllAsSent_일괄_발송_완료_상태로_변경한다() {
        // given
        Notification n1 = Notification.builder().receiverId(1L).isSent(false).build();
        Notification n2 = Notification.builder().receiverId(2L).isSent(false).build();
        List<Notification> notices = List.of(n1, n2);

        // when
        notificationService.markAllAsSent(notices);

        // then
        assertThat(n1.isSent()).isTrue();
        assertThat(n2.isSent()).isTrue();
        verify(notificationRepository).saveAll(notices);
    }
}
