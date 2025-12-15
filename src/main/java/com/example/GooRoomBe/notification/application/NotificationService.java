package com.example.GooRoomBe.notification.application;

import com.example.GooRoomBe.notification.domain.NotificationSetting;
import com.example.GooRoomBe.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSettingRepository settingRepository;

    @Transactional
    public void registerDeviceToken(String userId, String token) {
        NotificationSetting setting = settingRepository.findById(userId)
                .orElseGet(() -> new NotificationSetting(userId, token));

        if (!token.equals(setting.getFcmToken())) {
            setting.updateToken(token);
        }

        settingRepository.save(setting);
    }
}