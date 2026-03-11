package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSettingRepository settingRepository;
    private final FcmService fcmService;

    @Transactional
    public void registerDeviceToken(Long userId, String token) {
        NotificationSetting setting = settingRepository.findById(userId)
                .orElseGet(() -> settingRepository.save(new NotificationSetting(userId, token)));

        if (!token.equals(setting.getFcmToken())) {
            setting.updateToken(token);
            settingRepository.save(setting);

            if (setting.isAlarmOn()) {
                fcmService.subscribeToTopic(token, "notice");
            }
        }
    }

    @Transactional
    public void toggleAlarmSetting(Long userId, boolean isOn) {
        NotificationSetting setting = settingRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("알림 설정을 찾을 수 없습니다. ID: " + userId));

        setting.toggleAlarm(isOn);
        settingRepository.save(setting);

        if (setting.getFcmToken() != null) {
            if (isOn) {
                fcmService.subscribeToTopic(setting.getFcmToken(), "notice");
            } else {
                fcmService.unsubscribeFromTopic(setting.getFcmToken(), "notice");
            }
        }
    }
}