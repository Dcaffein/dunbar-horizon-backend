package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;
    private final NotificationSender notificationSender;

    @Transactional
    public void registerDeviceToken(Long userId, String newToken) {
        // 1. 유저의 기존 설정 조회 (없으면 새로 생성)
        NotificationSetting setting = settingRepository.findById(userId)
                .orElse(new NotificationSetting(userId, null));

        String oldToken = setting.getFcmToken();

        // 2. 토큰이 변경되지 않았다면 불필요한 DB 저장 및 API 호출 방지
        if (newToken.equals(oldToken)) {
            return;
        }

        // 3. 도메인 메서드를 통해 새로운 토큰으로 업데이트
        setting.updateToken(newToken);
        settingRepository.save(setting);

        // 4. 알림이 켜져있다면 전체 공지용(notice) 토픽 구독
        if (setting.isAlarmOn()) {
            notificationSender.subscribeToTopic(newToken, "notice");
        }
    }

    @Transactional
    public Notification savePendingNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Transactional
    public List<Notification> savePendingNotifications(List<Notification> notifications) {
        return notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markAsSent(String notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(notice -> {
                    notice.markAsSent();
                    notificationRepository.save(notice);
                });
    }

    @Transactional
    public void markAllAsSent(List<Notification> notifications) {
        notifications.forEach(Notification::markAsSent);
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void cleanupInvalidTokens(List<String> invalidTokens) {
        if (invalidTokens == null || invalidTokens.isEmpty()) return;

        List<NotificationSetting> settings = settingRepository.findAllByFcmTokenIn(invalidTokens);
        settings.forEach(setting -> setting.updateToken(null));
    }
}