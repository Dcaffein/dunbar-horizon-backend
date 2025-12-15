package com.example.GooRoomBe.notification.application;

import com.example.GooRoomBe.global.event.NotificationEvent;
import com.example.GooRoomBe.global.event.NotificationType;
import com.example.GooRoomBe.notification.domain.Notification;
import com.example.GooRoomBe.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("개인 알림 이벤트 수신 시: DB에 저장하고 FCM을 발송한 뒤, 성공 마킹을 한다")
    void handleNotificationRequest_Personal_Success() {
        // Given
        NotificationEvent event = new NotificationEvent(
                "receiverId", "Title", "Content", "/url", NotificationType.TRACE_REVEALED
        );

        // When
        listener.handleNotificationRequest(event);

        // Then
        InOrder inOrder = inOrder(notificationRepository, fcmService);

        // 최초 저장 (순서 1)
        inOrder.verify(notificationRepository).save(any(Notification.class));

        // FCM 발송 (순서 2)
        inOrder.verify(fcmService).sendNotification(any(NotificationEvent.class));

        // 성공 마킹 후 업데이트 저장 (순서 3)
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        inOrder.verify(notificationRepository).save(captor.capture());

        // 최종 상태 검증
        // 마지막에 저장된 객체는 반드시 isSent=true 여야 한다.
        Notification finalNotification = captor.getValue();
        assertThat(finalNotification.getReceiverId()).isEqualTo("receiverId");
        assertThat(finalNotification.isSent()).isTrue();
    }

    @Test
    @DisplayName("전체 공지(ALL) 이벤트 수신 시: FCM 토픽 전송을 호출한다")
    void handleNotificationRequest_Broadcast() {
        // Given
        NotificationEvent event = new NotificationEvent(
                "ALL", "Notice", "Content", "/url", NotificationType.NOTICE
        );

        // When
        listener.handleNotificationRequest(event);

        // Then
        verify(fcmService).sendToTopic(eq("notice"), eq(event));
        verify(fcmService, never()).sendNotification(any()); // 개인 발송은 안 함
    }

    @Test
    @DisplayName("FCM 발송 실패 시: 예외를 잡고 로그를 남기되, DB에는 isSent=false 상태로 남아야 한다")
    void handleNotificationRequest_FcmFail() {
        // Given
        NotificationEvent event = new NotificationEvent(
                "receiverId", "Title", "Content", "/url", NotificationType.TRACE_REVEALED
        );

        // FCM 발송 시 예외 발생 설정
        doThrow(new RuntimeException("FCM Error")).when(fcmService).sendNotification(any());

        // When
        listener.handleNotificationRequest(event);

        // Then
        //  저장은 되었어야 함
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));

        //  하지만 성공 마킹(업데이트)은 일어나지 않았어야 함 (호출 횟수가 1번이어야 함)
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}