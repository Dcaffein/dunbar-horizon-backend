package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.DeviceTokenDeregisteredEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
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
    private DeviceTokenService deviceTokenService;

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

        given(deviceTokenService.findTokensByUserIds(event.receiverIds()))
                .willReturn(List.of("token-1", "token-2"));

        List<String> invalidTokens = List.of("token-2");
        given(notificationSender.sendMulticast(eq(event), anyList())).willReturn(invalidTokens);

        // when
        eventListener.handleNotificationRequest(event);

        // then
        ArgumentCaptor<List<String>> tokenCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSender).sendMulticast(eq(event), tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).containsExactlyInAnyOrder("token-1", "token-2");

        verify(deviceTokenService).cleanupInvalidTokens(invalidTokens);
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

        given(deviceTokenService.findTokensByUserIds(event.receiverIds()))
                .willReturn(List.of());

        // when
        eventListener.handleNotificationRequest(event);

        // then
        verify(notificationSender, never()).sendMulticast(any(), any());
    }

    @Test
    @DisplayName("handleDeviceTokenDeregistration: BEFORE_COMMIT 핸들러에서 토큰을 삭제한다")
    void handleDeviceTokenDeregistration_토큰을_삭제한다() {
        // given
        DeviceTokenDeregisteredEvent event = new DeviceTokenDeregisteredEvent("token-to-remove");

        // when
        eventListener.handleDeviceTokenDeregistration(event);

        // then
        verify(deviceTokenService).removeDeviceToken("token-to-remove");
    }
}
