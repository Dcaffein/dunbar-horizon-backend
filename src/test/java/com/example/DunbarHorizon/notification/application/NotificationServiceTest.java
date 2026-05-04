package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSettingRepository settingRepository;

    @Test
    @DisplayName("savePendingNotification: 알림 내역을 DB에 단건 저장한다")
    void savePendingNotification_Success() {
        // given
        Notification notice = Notification.builder().title("테스트").build();
        given(notificationRepository.save(notice)).willReturn(notice);

        // when
        Notification result = notificationService.savePendingNotification(notice);

        // then
        assertThat(result).isEqualTo(notice);
        verify(notificationRepository).save(notice);
    }

    @Test
    @DisplayName("markAsSent: 도메인 엔티티를 조회하여 발송 완료 상태로 변경한다")
    void markAsSent_Success() {
        // given
        Notification notice = Notification.builder().receiverId(1L).isSent(false).build();
        given(notificationRepository.findById("notice-id")).willReturn(Optional.of(notice));

        // when
        notificationService.markAsSent("notice-id");

        // then
        assertThat(notice.isSent()).isTrue(); // markAsSent() 호출 여부 확인
        verify(notificationRepository).save(notice);
    }

    @Test
    @DisplayName("markAllAsSent: 다수의 도메인 엔티티를 일괄 발송 완료 상태로 변경한다")
    void markAllAsSent_Success() {
        // given
        Notification n1 = Notification.builder().receiverId(1L).isSent(false).build();
        Notification n2 = Notification.builder().receiverId(2L).isSent(false).build();
        List<Notification> notices = List.of(n1, n2);

        // when
        notificationService.markAllAsSent(notices);

        // then
        assertThat(n1.isSent()).isTrue();
        assertThat(n2.isSent()).isTrue();
        verify(notificationRepository).saveAll(notices);
    }

    @Test
    @DisplayName("cleanupInvalidTokens: 죽은 토큰을 가진 설정 엔티티를 찾아 토큰을 null 처리한다")
    void cleanupInvalidTokens_Success() {
        // given
        NotificationSetting setting = new NotificationSetting(1L, "dead-token");
        List<String> invalidTokens = List.of("dead-token");

        given(settingRepository.findAllByFcmTokenIn(invalidTokens)).willReturn(List.of(setting));

        // when
        notificationService.cleanupInvalidTokens(invalidTokens);

        // then
        assertThat(setting.getFcmToken()).isNull(); // updateToken(null) 호출 여부 확인
    }
}