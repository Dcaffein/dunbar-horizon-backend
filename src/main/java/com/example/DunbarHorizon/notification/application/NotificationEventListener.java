package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.DeviceTokenDeregisteredEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.application.port.out.PushMessage;
import com.example.DunbarHorizon.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationSender notificationSender;
    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNotificationRequest(NotificationEvent event) {
        List<Notification> notifications = event.receiverIds().stream()
                .map(id -> createSingleNotification(id, event))
                .toList();
        notificationService.saveNotifications(notifications);

        List<String> tokens = deviceTokenService.findTokensByUserIds(event.receiverIds());

        if (tokens.isEmpty()) {
            log.info("유효한 FCM 토큰이 없어 FCM 발송을 생략합니다. 대상: {}", event.receiverIds());
            return;
        }

        try {
            List<String> invalidTokens = notificationSender.send(pushMessage(event), tokens);
            deviceTokenService.cleanupInvalidTokens(invalidTokens);
        } catch (Exception e) {
            log.error("FCM 벌크 전송 중 시스템 에러: {}", e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDeviceTokenDeregistration(DeviceTokenDeregisteredEvent event) {
        deviceTokenService.removeDeviceToken(event.token());
    }

    private PushMessage pushMessage(NotificationEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", event.type().name());
        if (event.metadata() != null) {
            data.putAll(event.metadata());
        }
        return new PushMessage(event.title(), event.content(), data);
    }

    private Notification createSingleNotification(Long receiverId, NotificationEvent event) {
        return Notification.builder()
                .receiverId(receiverId)
                .title(event.title())
                .content(event.content())
                .type(event.type())
                .metadata(event.metadata())
                .build();
    }
}
