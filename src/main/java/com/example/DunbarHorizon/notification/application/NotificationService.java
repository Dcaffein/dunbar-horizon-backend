package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.event.DeviceTokenRegisteredEvent;
import com.example.DunbarHorizon.notification.domain.repository.DeviceTokenRepository;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
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
    private final DeviceTokenRepository deviceTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerDeviceToken(Long userId, String token) {
        if (deviceTokenRepository.existsByUserIdAndFcmToken(userId, token)) {
            return;
        }
        deviceTokenRepository.deleteAllByUserId(userId);
        deviceTokenRepository.save(new DeviceToken(userId, token));
        eventPublisher.publishEvent(new DeviceTokenRegisteredEvent(token));
    }

    @Transactional
    public void removeDeviceToken(String fcmToken) {
        deviceTokenRepository.deleteByFcmToken(fcmToken);
    }

    @Transactional
    public void removeDeviceTokenByUserId(Long userId) {
        deviceTokenRepository.deleteAllByUserId(userId);
    }

    public boolean isTokenRegisteredForUser(Long userId, String fcmToken) {
        return deviceTokenRepository.existsByUserIdAndFcmToken(userId, fcmToken);
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
        deviceTokenRepository.deleteAllByFcmTokenIn(invalidTokens);
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

    public void deleteNotification(String notificationId, Long currentUserId) {
        Notification notice = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        notice.requireOwnership(currentUserId);
        notificationRepository.deleteById(notificationId);
    }
}
