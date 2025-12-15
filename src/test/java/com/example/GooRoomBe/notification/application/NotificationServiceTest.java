package com.example.GooRoomBe.notification.application;

import com.example.GooRoomBe.notification.domain.NotificationSetting;
import com.example.GooRoomBe.notification.repository.NotificationSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationSettingRepository settingRepository;
    @InjectMocks private NotificationService notificationService;

    @Test
    @DisplayName("토큰 등록: 기존 설정이 없으면 새로 생성해서 저장한다")
    void registerDeviceToken_NewUser_ShouldCreate() {
        // Given
        String userId = "user1";
        String token = "new-token";
        given(settingRepository.findById(userId)).willReturn(Optional.empty());

        // When
        notificationService.registerDeviceToken(userId, token);

        // Then
        verify(settingRepository).save(any(NotificationSetting.class));
    }

    @Test
    @DisplayName("토큰 등록: 기존 토큰과 다르면 업데이트된 상태로 저장한다")
    void registerDeviceToken_ExistingUser_ShouldUpdate() {
        // Given
        String userId = "user1";
        String oldToken = "old-token";
        String newToken = "new-token";

        // Spy 대신 일반 객체 사용
        NotificationSetting existingSetting = new NotificationSetting(userId, oldToken);
        given(settingRepository.findById(userId)).willReturn(Optional.of(existingSetting));

        // When
        notificationService.registerDeviceToken(userId, newToken);

        // Then
        // "Repository.save()가 호출될 때 넘어간 인자(Argument)를 낚아채겠다(Capture)"
        ArgumentCaptor<NotificationSetting> captor = ArgumentCaptor.forClass(NotificationSetting.class);
        verify(settingRepository).save(captor.capture());

        // 낚아챈 객체의 값이 진짜 바뀌었는지 검증
        NotificationSetting savedSetting = captor.getValue();
        assertThat(savedSetting.getFcmToken()).isEqualTo(newToken); // 값이 new-token으로 바뀌었는지 확인
    }
}