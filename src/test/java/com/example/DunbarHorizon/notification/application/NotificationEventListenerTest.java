package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.DeviceTokenDeregisteredEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @InjectMocks
    private NotificationEventListener eventListener;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Test
    @DisplayName("handleNotificationRequest: 알림 요청 이벤트를 알림 발송으로 넘긴다")
    void handleNotificationRequest_알림_발송으로_넘긴다() {
        // given
        NotificationEvent event = NotificationEvent.builder()
                .receiverIds(List.of(1L, 2L))
                .title("제목")
                .content("내용")
                .type(NotificationType.BUZZ_ARRIVAL)
                .metadata(Map.of("buzzId", 7L))
                .build();

        // when
        eventListener.handleNotificationRequest(event);

        // then
        verify(notificationService).send(List.of(1L, 2L), "제목", "내용",
                NotificationType.BUZZ_ARRIVAL, Map.of("buzzId", 7L));
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
