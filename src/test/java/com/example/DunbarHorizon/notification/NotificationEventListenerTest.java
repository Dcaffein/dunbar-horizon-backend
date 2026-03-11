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

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @InjectMocks
    private NotificationEventListener eventListener;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private FcmService fcmService;

    @Test
    @DisplayName("알림 요청 이벤트가 오면 먼저 DB에 벌크로 저장하고 멀티캐스트 전송을 호출한다")
    void handleNotificationRequest_BulkSaveAndSend() {
        // given
        // 단일 수신자 생성자를 사용해도 내부적으로는 List로 관리됨
        NotificationEvent event = new NotificationEvent(1L, "제목", "내용", NotificationType.TRACE_REVEALED, null);

        Notification savedNotification = Notification.builder()
                .receiverId(1L)
                .title("제목")
                .build();
        ReflectionTestUtils.setField(savedNotification, "id", "generated-id");

        // saveAll stubbing: 리스트를 받아 리스트를 반환함
        given(notificationRepository.saveAll(anyList())).willReturn(List.of(savedNotification));

        // when
        eventListener.handleNotificationRequest(event);

        // then
        //  벌크 저장 확인
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> capturedNotifications = captor.getValue();

        // 리스트 자체에 대한 검증 (임포트 수정 후 가능)
        assertThat(capturedNotifications).hasSize(1);
        assertThat(capturedNotifications.get(0).isSent()).isFalse();

        verify(fcmService).sendMulticastNotification(eq(event), eq(List.of(savedNotification)));
    }
}