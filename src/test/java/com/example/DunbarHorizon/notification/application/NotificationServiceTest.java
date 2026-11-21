package com.example.DunbarHorizon.notification.application;

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

    @Test
    @DisplayName("readNotification: 소유자 확인 뒤 읽음만 갱신하고 읽은 상태로 돌려준다")
    void readNotification_읽음만_갱신한다() {
        // given
        Notification notice = Notification.builder().receiverId(1L).build();
        given(notificationRepository.findRetainedById(eq(5L), any(LocalDateTime.class))).willReturn(Optional.of(notice));
        given(notificationRepository.markAsRead(eq(5L), any(LocalDateTime.class))).willReturn(1);

        // when
        Notification result = notificationService.readNotification(5L, 1L);

        // then
        assertThat(result.isRead()).isTrue();
    }

    @Test
    @DisplayName("readNotification: 남의 알림이면 읽음 갱신 없이 거부한다")
    void readNotification_남의_알림이면_거부한다() {
        // given
        Notification notice = Notification.builder().receiverId(2L).build();
        given(notificationRepository.findRetainedById(eq(5L), any(LocalDateTime.class))).willReturn(Optional.of(notice));

        // when & then
        assertThatThrownBy(() -> notificationService.readNotification(5L, 1L))
                .isInstanceOf(NotificationAccessDeniedException.class);
        verify(notificationRepository, never()).markAsRead(any(), any());
    }

    @Test
    @DisplayName("readNotification: 보관 기간이 지난 알림이면 찾을 수 없음으로 끝내고 읽음 갱신을 하지 않는다")
    void readNotification_보관_기간이_지나면_찾을_수_없다() {
        // given
        given(notificationRepository.findRetainedById(eq(5L), any(LocalDateTime.class))).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.readNotification(5L, 1L))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(notificationRepository, never()).markAsRead(any(), any());
    }

    @Test
    @DisplayName("readNotification: 조회와 읽음 갱신은 같은 기준 시각을 쓴다")
    void readNotification_조회와_갱신이_같은_기준_시각을_쓴다() {
        // given
        Notification notice = Notification.builder().receiverId(1L).build();
        given(notificationRepository.findRetainedById(eq(5L), any(LocalDateTime.class))).willReturn(Optional.of(notice));
        given(notificationRepository.markAsRead(eq(5L), any(LocalDateTime.class))).willReturn(1);

        // when
        notificationService.readNotification(5L, 1L);

        // then
        ArgumentCaptor<LocalDateTime> lookupAsOf = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> updateAsOf = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).findRetainedById(eq(5L), lookupAsOf.capture());
        verify(notificationRepository).markAsRead(eq(5L), updateAsOf.capture());
        assertThat(updateAsOf.getValue()).isEqualTo(lookupAsOf.getValue());
    }

    @Test
    @DisplayName("deleteNotification: 없는 알림이면 찾을 수 없음으로 끝내고 삭제하지 않는다")
    void deleteNotification_없는_알림이면_찾을_수_없다() {
        // given
        given(notificationRepository.findById(5L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(5L, 1L))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(notificationRepository, never()).deleteById(any());
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
        ArgumentCaptor<LocalDateTime> asOf = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).purgeExpired(asOf.capture());
        assertThat(asOf.getValue()).isBetween(before, LocalDateTime.now());
    }
}
