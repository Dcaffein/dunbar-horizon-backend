package com.example.DunbarHorizon.notification;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.application.FcmService;
import com.example.DunbarHorizon.notification.application.NotificationEventListener;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @InjectMocks
    private NotificationEventListener eventListener;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private FcmService fcmService;

    @Test
    @DisplayName("특정 사용자 알림 이벤트: DB에 벌크로 저장하고 멀티캐스트 전송을 호출한다")
    void handleNotificationRequest_SpecificUser_BulkSaveAndSend() {
        // given
        // 변경된 Enum 구조에 맞춰 단일 수신자 이벤트 생성
        NotificationEvent event = new NotificationEvent(
                1L, "제목", "내용", NotificationType.TRACE_REVEALED, null
        );

        Notification savedNotification = Notification.builder()
                .receiverId(1L)
                .title("제목")
                .build();
        ReflectionTestUtils.setField(savedNotification, "id", "generated-id");

        given(notificationRepository.saveAll(anyList())).willReturn(List.of(savedNotification));

        // when
        eventListener.handleNotificationRequest(event);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> capturedNotifications = captor.getValue();
        assertThat(capturedNotifications).hasSize(1);
        assertThat(capturedNotifications.get(0).getReceiverId()).isEqualTo(1L); // 수신자 확인
        assertThat(capturedNotifications.get(0).isSent()).isFalse();

        // 브로드캐스트가 아닌 멀티캐스트가 호출되었는지 확인
        verify(fcmService).sendMulticastNotification(eq(event), eq(List.of(savedNotification)));
        verifyNoMoreInteractions(fcmService); // 다른 FCM 메서드는 안 불려야 함
    }

    @Test
    @DisplayName("브로드캐스트(공지) 알림 이벤트: 단건 저장 후 브로드캐스트 전송을 호출한다")
    void handleNotificationRequest_Broadcast_SaveAndBroadcast() {
        // given
        // 정적 팩토리 메서드(toAll)를 사용하여 브로드캐스트 이벤트 생성
        NotificationEvent event = NotificationEvent.toAll(
                "공지 제목", "공지 내용", NotificationType.NOTICE, null
        );

        Notification savedNotification = Notification.builder()
                .receiverId(null) // null 검증
                .title("공지 제목")
                .build();
        ReflectionTestUtils.setField(savedNotification, "id", "broadcast-id");

        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

        // when
        eventListener.handleNotificationRequest(event);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification capturedNotification = captor.getValue();
        // receiverId가 0L이 아니라 null로 세팅되었는지 확인
        assertThat(capturedNotification.getReceiverId()).isNull();
        assertThat(capturedNotification.getTitle()).isEqualTo("공지 제목");

        // 멀티캐스트가 아닌 브로드캐스트가 호출되었는지 확인
        verify(fcmService).broadcastNotification(eq(event), eq("broadcast-id")); // savedNotification.getId() 가 들어가야 함. 현재 stub이 완벽하지 않아 null이 들어갈수도 있으나 로직흐름 확인.
        verifyNoMoreInteractions(fcmService);
    }
}