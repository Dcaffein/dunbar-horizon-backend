package com.example.DunbarHorizon.notification;

import com.example.DunbarHorizon.notification.application.FcmService;
import com.example.DunbarHorizon.notification.application.NotificationService;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;
    @Mock
    private NotificationSettingRepository settingRepository;
    @Mock
    private FcmService fcmService;

    private final Long userId = 1L;

    @Test
    @DisplayName("새로운 토큰이 들어오고 알림이 켜져있다면 토픽을 구독한다")
    void registerDeviceToken_NewToken_Subscribe() {
        // given
        NotificationSetting setting = new NotificationSetting(userId, "old-token");
        setting.toggleAlarm(true);
        given(settingRepository.findById(userId)).willReturn(Optional.of(setting));

        // when
        notificationService.registerDeviceToken(userId, "new-token");

        // then
        assertThat(setting.getFcmToken()).isEqualTo("new-token");
        verify(fcmService).subscribeToTopic("new-token", "notice");
        verify(settingRepository).save(setting);
    }

    @Test
    @DisplayName("토큰이 기존과 같다면 아무런 작업도 하지 않는다")
    void registerDeviceToken_SameToken_DoNothing() {
        // given
        NotificationSetting setting = new NotificationSetting(userId, "same-token");
        given(settingRepository.findById(userId)).willReturn(Optional.of(setting));

        // when
        notificationService.registerDeviceToken(userId, "same-token");

        // then
        verify(fcmService, never()).subscribeToTopic(anyString(), anyString());
        verify(settingRepository, never()).save(any());
    }
}