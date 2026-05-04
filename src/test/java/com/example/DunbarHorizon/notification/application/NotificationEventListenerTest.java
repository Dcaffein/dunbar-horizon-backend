package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
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
    private NotificationSender notificationSender; // 변경: FcmService -> 인터페이스(Port)

    @Mock
    private NotificationService historyService; // 트랜잭션 전담 서비스 모킹

    @Mock
    private NotificationSettingRepository settingRepository;

    @Test
    @DisplayName("특정 사용자 알림: 유효한 토큰을 필터링하여 발송하고, 죽은 토큰을 청소한다")
    void handleNotificationRequest_SpecificUser_MulticastFlow() {
        // given
        NotificationEvent event = NotificationEvent.builder()
                .receiverIds(List.of(1L,2L,3L))
                .title("제목")
                .content("내용")
                .type(NotificationType.BUZZ_ARRIVAL)
                .metadata(null)
                .build();

        // 유저 1: 정상 수신 / 유저 2: 알림 끔 / 유저 3: 토큰 없음
        NotificationSetting s1 = new NotificationSetting(1L, "token-1"); // isOn=true
        NotificationSetting s2 = new NotificationSetting(2L, "token-2");
        s2.toggleAlarm(false);
        NotificationSetting s3 = new NotificationSetting(3L, null); // isOn=true

        given(settingRepository.findAllByUserIdIn(event.receiverIds())).willReturn(List.of(s1, s2, s3));

        // DB에 저장된 상태라고 가정
        Notification savedNotice1 = Notification.builder().receiverId(1L).build();
        given(historyService.savePendingNotifications(anyList())).willReturn(List.of(savedNotice1));

        // FCM 발송 결과, "token-1"이 죽은 토큰으로 판명되었다고 가정
        List<String> invalidTokens = List.of("token-1");
        given(notificationSender.sendMulticast(eq(event), anyList())).willReturn(invalidTokens);

        // when
        eventListener.handleNotificationRequest(event);

        // then
        // 1. 토큰 필터링 검증: s1의 토큰만 발송 목록에 포함되어야 함
        ArgumentCaptor<List<String>> tokenCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSender).sendMulticast(eq(event), tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).containsExactly("token-1");

        // 2. 발송 후 처리(Tx) 검증: 성공 처리 및 청소 메서드가 불렸는지 확인
        verify(historyService).markAllAsSent(List.of(savedNotice1));
        verify(historyService).cleanupInvalidTokens(invalidTokens);
    }

    @Test
    @DisplayName("브로드캐스트 알림: 단건 저장 후 발송하고 상태를 업데이트한다")
    void handleNotificationRequest_Broadcast_SaveAndBroadcastFlow() {
        // given
        NotificationEvent event = NotificationEvent.toAll(
                "공지 제목", "공지 내용", NotificationType.NOTICE, null
        );

        Notification savedNotice = Notification.builder().receiverId(null).title("공지 제목").build();
        ReflectionTestUtils.setField(savedNotice, "id", "broadcast-id");

        given(historyService.savePendingNotification(any(Notification.class))).willReturn(savedNotice);

        // when
        eventListener.handleNotificationRequest(event);

        // then
        verify(historyService).savePendingNotification(any(Notification.class));
        verify(notificationSender).sendBroadcast(event); // Port 호출 확인
        verify(historyService).markAsSent("broadcast-id"); // 사후 처리 확인
    }
}