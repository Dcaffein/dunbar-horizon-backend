package com.example.DunbarHorizon.notification;

import com.example.DunbarHorizon.notification.application.FcmService;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
import com.google.firebase.messaging.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private NotificationSettingRepository settingRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("멀티캐스트 전송 성공 시 모든 알림 엔티티의 상태를 '전송 완료'로 변경한다")
    void dispatchMulticast_Success_UpdateAllStatus() throws FirebaseMessagingException {
        // given
        Notification n1 = Notification.builder().receiverId(1L).isSent(false).build();
        Notification n2 = Notification.builder().receiverId(2L).isSent(false).build();
        List<Notification> savedNotifications = List.of(n1, n2);

        MulticastMessage message = MulticastMessage.builder().addAllTokens(List.of("t1", "t2")).build();

        // BatchResponse 스터빙
        BatchResponse mockResponse = mock(BatchResponse.class);
        given(mockResponse.getFailureCount()).willReturn(0);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(mockResponse);

        // when
        fcmService.dispatchMulticast(message, List.of("t1", "t2"), Map.of("t1", 1L, "t2", 2L), savedNotifications);

        // then
        assertThat(n1.isSent()).isTrue();
        assertThat(n2.isSent()).isTrue();
        verify(notificationRepository).saveAll(savedNotifications);
    }

    @Test
    @DisplayName("멀티캐스트 중 특정 토큰이 만료(UNREGISTERED)되면 해당 유저의 토큰을 제거한다")
    void dispatchMulticast_PartialFailure_HandleInvalidToken() throws FirebaseMessagingException {
        // given
        String expiredToken = "expired-t1";
        Long userId = 100L;
        Notification n1 = Notification.builder().receiverId(userId).build();

        // 1. 실패 응답 구성
        SendResponse failResponse = mock(SendResponse.class);
        FirebaseMessagingException fme = mock(FirebaseMessagingException.class);
        given(failResponse.isSuccessful()).willReturn(false);
        given(failResponse.getException()).willReturn(fme);
        given(fme.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getFailureCount()).willReturn(1);
        given(batchResponse.getResponses()).willReturn(List.of(failResponse));

        given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

        // 2. 설정 엔티티 스터빙
        NotificationSetting setting = new NotificationSetting(userId, expiredToken);
        given(settingRepository.findById(userId)).willReturn(Optional.of(setting));

        // when
        fcmService.dispatchMulticast(
                mock(MulticastMessage.class),
                List.of(expiredToken),
                Map.of(expiredToken, userId),
                List.of(n1)
        );

        // then
        assertThat(setting.getFcmToken()).isNull();
        verify(settingRepository).save(setting);
    }

    @Test
    @DisplayName("토픽 전송 성공 시 알림 상태를 업데이트한다")
    void dispatchToBroadcast_Success_UpdateStatus() throws FirebaseMessagingException {
        // given
        String notificationId = "notice-1";
        Notification notification = Notification.builder().id(notificationId).isSent(false).build();
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        fcmService.dispatchToBroadcast(mock(Message.class), notificationId);

        // then
        verify(firebaseMessaging).send(any(Message.class));
        assertThat(notification.isSent()).isTrue();
        verify(notificationRepository).save(notification);
    }
}