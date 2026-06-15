package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.DeviceTokenDeregisteredEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.repository.DeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @InjectMocks
    private NotificationEventListener eventListener;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    @DisplayName("멀티캐스트: 수신자들의 토큰을 조회하여 발송하고 죽은 토큰을 정리한다")
    void handleNotificationRequest_멀티캐스트_발송_후_죽은_토큰_정리() {
        // given
        NotificationEvent event = NotificationEvent.builder()
                .receiverIds(List.of(1L, 2L, 3L))
                .title("제목")
                .content("내용")
                .type(NotificationType.BUZZ_ARRIVAL)
                .metadata(null)
                .build();

        given(deviceTokenRepository.findAllFcmTokensByUserIdIn(event.receiverIds()))
                .willReturn(List.of("token-1", "token-2"));

        Notification saved = Notification.builder().receiverId(1L).build();
        given(notificationService.savePendingNotifications(anyList())).willReturn(List.of(saved));

        List<String> invalidTokens = List.of("token-2");
        given(notificationSender.sendMulticast(eq(event), anyList())).willReturn(invalidTokens);

        // when
        eventListener.handleNotificationRequest(event);

        // then
        ArgumentCaptor<List<String>> tokenCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSender).sendMulticast(eq(event), tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).containsExactlyInAnyOrder("token-1", "token-2");

        verify(notificationService).markAllAsSent(List.of(saved));
        verify(notificationService).cleanupInvalidTokens(invalidTokens);
    }

    @Test
    @DisplayName("멀티캐스트: 등록된 토큰이 없으면 FCM 발송을 생략한다")
    void handleNotificationRequest_토큰없으면_FCM_발송_생략() {
        // given
        NotificationEvent event = NotificationEvent.builder()
                .receiverIds(List.of(1L))
                .title("제목")
                .content("내용")
                .type(NotificationType.BUZZ_ARRIVAL)
                .metadata(null)
                .build();

        given(notificationService.savePendingNotifications(anyList())).willReturn(List.of());
        given(deviceTokenRepository.findAllFcmTokensByUserIdIn(event.receiverIds()))
                .willReturn(List.of());

        // when
        eventListener.handleNotificationRequest(event);

        // then
        verify(notificationSender, never()).sendMulticast(any(), any());
    }

    @Test
    @DisplayName("브로드캐스트: 단건 저장 후 토픽으로 발송하고 상태를 업데이트한다")
    void handleNotificationRequest_브로드캐스트_저장_후_발송() {
        // given
        NotificationEvent event = NotificationEvent.toAll(
                "공지 제목", "공지 내용", NotificationType.NOTICE, null
        );

        Notification savedNotice = Notification.builder().receiverId(null).title("공지 제목").build();
        ReflectionTestUtils.setField(savedNotice, "id", "broadcast-id");
        given(notificationService.savePendingNotification(any(Notification.class))).willReturn(savedNotice);

        // when
        eventListener.handleNotificationRequest(event);

        // then
        verify(notificationService).savePendingNotification(any(Notification.class));
        verify(notificationSender).sendBroadcast(event);
        verify(notificationService).markAsSent("broadcast-id");
    }

    @Test
    @DisplayName("handleDeviceTokenDeregistration: BEFORE_COMMIT 핸들러에서 토큰을 삭제한다")
    void handleDeviceTokenDeregistration_토큰을_삭제한다() {
        // given
        DeviceTokenDeregisteredEvent event = new DeviceTokenDeregisteredEvent("token-to-remove");

        // when
        eventListener.handleDeviceTokenDeregistration(event);

        // then
        verify(notificationService).removeDeviceToken("token-to-remove");
    }
}
