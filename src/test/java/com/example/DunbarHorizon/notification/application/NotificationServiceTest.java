package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.application.port.out.PushMessage;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.exception.NotificationAccessDeniedException;
import com.example.DunbarHorizon.notification.domain.exception.NotificationNotFoundException;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private NotificationSender notificationSender;

    @Test
    @DisplayName("send: 수신자마다 알림을 저장하고 기기들에 푸시를 보낸 뒤 무효 토큰을 정리한다")
    void send_알림을_저장하고_푸시를_보낸_뒤_무효_토큰을_정리한다() {
        // given
        given(deviceTokenService.findTokensByUserIds(List.of(1L, 2L))).willReturn(List.of("token-1", "token-2"));
        given(notificationSender.send(any(PushMessage.class), eq(List.of("token-1", "token-2"))))
                .willReturn(List.of("token-2"));

        // when
        notificationService.send(List.of(1L, 2L), "제목", "내용", NotificationType.BUZZ_ARRIVAL, Map.of("buzzId", 7L));

        // then
        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Notification::getReceiverId).containsExactly(1L, 2L);
        verify(notificationSender).send(
                new PushMessage("제목", "내용", Map.of("type", "BUZZ_ARRIVAL", "buzzId", 7L)),
                List.of("token-1", "token-2"));
        verify(deviceTokenService).cleanupInvalidTokens(List.of("token-2"));
    }

    @Test
    @DisplayName("send: 등록된 기기 토큰이 없으면 알림만 저장하고 푸시는 보내지 않는다")
    void send_기기_토큰이_없으면_푸시를_보내지_않는다() {
        // given
        given(deviceTokenService.findTokensByUserIds(List.of(1L))).willReturn(List.of());

        // when
        notificationService.send(List.of(1L), "제목", "내용", NotificationType.BUZZ_ARRIVAL, null);

        // then
        verify(notificationRepository).saveAll(any());
        verify(notificationSender, never()).send(any(), any());
    }

    @Test
    @DisplayName("send: 푸시 발송이 실패해도 저장한 알림은 남기고 예외를 밖으로 내지 않는다")
    void send_푸시_실패는_밖으로_내지_않는다() {
        // given
        given(deviceTokenService.findTokensByUserIds(List.of(1L))).willReturn(List.of("token-1"));
        given(notificationSender.send(any(PushMessage.class), any())).willThrow(new IllegalStateException("FCM down"));

        // when
        notificationService.send(List.of(1L), "제목", "내용", NotificationType.BUZZ_ARRIVAL, null);

        // then
        verify(notificationRepository).saveAll(any());
        verify(deviceTokenService, never()).cleanupInvalidTokens(any());
    }

    @Test
    @DisplayName("readNotification: 내 알림을 읽음으로 바꿔 저장하고 돌려준다")
    void readNotification_읽음으로_바꿔_저장한다() {
        // given
        Notification notification = Notification.builder().receiverId(1L).build();
        given(notificationRepository.findRetainedById(eq(5L), any(LocalDateTime.class))).willReturn(Optional.of(notification));
        given(notificationRepository.save(notification)).willReturn(notification);

        // when
        Notification result = notificationService.readNotification(5L, 1L);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().isRead()).isTrue();
        assertThat(result.isRead()).isTrue();
    }

    @Test
    @DisplayName("readNotification: 남의 알림이면 저장 없이 거부한다")
    void readNotification_남의_알림이면_거부한다() {
        // given
        Notification notification = Notification.builder().receiverId(2L).build();
        given(notificationRepository.findRetainedById(eq(5L), any(LocalDateTime.class))).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.readNotification(5L, 1L))
                .isInstanceOf(NotificationAccessDeniedException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("readNotification: 보관 기간이 지난 알림이면 찾을 수 없음으로 끝내고 저장하지 않는다")
    void readNotification_보관_기간이_지나면_찾을_수_없다() {
        // given
        given(notificationRepository.findRetainedById(eq(5L), any(LocalDateTime.class))).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.readNotification(5L, 1L))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteNotification: 내 알림이면 지운다")
    void deleteNotification_내_알림이면_지운다() {
        // given
        Notification notification = Notification.builder().receiverId(1L).build();
        given(notificationRepository.findById(5L)).willReturn(Optional.of(notification));

        // when
        notificationService.deleteNotification(5L, 1L);

        // then
        verify(notificationRepository).delete(notification);
    }

    @Test
    @DisplayName("deleteNotification: 남의 알림이면 지우지 않고 거부한다")
    void deleteNotification_남의_알림이면_거부한다() {
        // given
        Notification notification = Notification.builder().receiverId(2L).build();
        given(notificationRepository.findById(5L)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(5L, 1L))
                .isInstanceOf(NotificationAccessDeniedException.class);
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteNotification: 없는 알림이면 찾을 수 없음으로 끝내고 삭제하지 않는다")
    void deleteNotification_없는_알림이면_찾을_수_없다() {
        // given
        given(notificationRepository.findById(5L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(5L, 1L))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("purgeExpiredNotifications: 현재 시각을 기준 시각으로 보관 기간이 지난 알림을 정리한다")
    void purgeExpiredNotifications_현재_시각_기준으로_정리한다() {
        // given
        LocalDateTime before = LocalDateTime.now();
        given(notificationRepository.purgeExpired(any(LocalDateTime.class))).willReturn(3);

        // when
        notificationService.purgeExpiredNotifications();

        // then
        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).purgeExpired(now.capture());
        assertThat(now.getValue()).isBetween(before, LocalDateTime.now());
    }
}
