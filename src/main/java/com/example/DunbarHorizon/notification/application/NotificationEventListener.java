package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.DeviceTokenDeregisteredEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.event.DeviceTokenRegisteredEvent;
import com.example.DunbarHorizon.notification.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationSender notificationSender;
    private final NotificationService notificationService;
    private final DeviceTokenRepository deviceTokenRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNotificationRequest(NotificationEvent event) {

        if (event.isAnnouncement()) {
            Notification notice = createSingleNotification(null, event);
            notice = notificationService.savePendingNotification(notice);
            notificationSender.sendBroadcast(event);
            notificationService.markAsSent(notice.getId());
            return;
        }

        List<Notification> notifications = event.receiverIds().stream()
                .map(id -> createSingleNotification(id, event))
                .toList();
        List<Notification> savedNotifications = notificationService.savePendingNotifications(notifications);

        List<String> tokens = deviceTokenRepository.findAllFcmTokensByUserIdIn(event.receiverIds());

        if (tokens.isEmpty()) {
            log.info("유효한 FCM 토큰이 없어 FCM 발송을 생략합니다. 대상: {}", event.receiverIds());
            return;
        }

        try {
            List<String> invalidTokens = notificationSender.sendMulticast(event, tokens);
            notificationService.markAllAsSent(savedNotifications);
            notificationService.cleanupInvalidTokens(invalidTokens);
        } catch (Exception e) {
            log.error("FCM 벌크 전송 중 시스템 에러: {}", e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeviceTokenRegistration(DeviceTokenRegisteredEvent event) {
        try {
            notificationSender.subscribeToTopic(event.token(), "notice");
            log.info("새로운 기기 토큰이 공지사항(notice) 토픽에 성공적으로 구독되었습니다.");
        } catch (Exception e) {
            log.error("FCM 토픽 구독 중 에러 발생: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDeviceTokenDeregistration(DeviceTokenDeregisteredEvent event) {
        notificationService.removeDeviceToken(event.fcmToken());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeviceTokenDeregistrationAsync(DeviceTokenDeregisteredEvent event) {
        try {
            notificationSender.unsubscribeFromTopic(event.fcmToken(), "notice");
        } catch (Exception e) {
            log.error("FCM 토픽 해지 중 에러 발생: {}", e.getMessage());
        }
    }

    private Notification createSingleNotification(Long receiverId, NotificationEvent event) {
        return Notification.builder()
                .receiverId(receiverId)
                .title(event.title())
                .content(event.content())
                .type(event.type())
                .metadata(event.metadata())
                .isSent(false)
                .build();
    }
}
