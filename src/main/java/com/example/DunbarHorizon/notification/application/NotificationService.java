package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.event.DeviceTokenRegisteredEvent;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerDeviceToken(Long userId, String newToken) {
        NotificationSetting setting = settingRepository.findById(userId)
                .orElse(new NotificationSetting(userId, null));

        String oldToken = setting.getFcmToken();

        if (newToken.equals(oldToken)) {
            return;
        }

        setting.updateToken(newToken);
        settingRepository.save(setting);

        eventPublisher.publishEvent(new DeviceTokenRegisteredEvent(newToken, setting.isAlarmOn()));
    }

    public Notification savePendingNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public List<Notification> savePendingNotifications(List<Notification> notifications) {
        return notificationRepository.saveAll(notifications);
    }

    public void markAsSent(String notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(notice -> {
                    notice.markAsSent();
                    notificationRepository.save(notice);
                });
    }

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

    public Notification readNotification(String notificationId, Long currentUserId) {
        Notification notice = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        notice.read(currentUserId);

        return notificationRepository.save(notice);
    }

    public Slice<Notification> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findAllByReceiverId(userId, pageable);
    }

    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }
}