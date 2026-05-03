package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationRequest(NotificationEvent event) {

        if (event.isAnnouncement()) {
            Notification notice = createSingleNotification(null, event);
            notificationRepository.save(notice);
            fcmService.broadcastNotification(event, notice.getId());
            return;
        }

        List<Notification> notifications = event.receiverIds().stream()
                .map(id -> createSingleNotification(id, event))
                .toList();

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        try {
            fcmService.sendMulticastNotification(event, savedNotifications);
        } catch (Exception e) {
            log.error("FCM 벌크 전송 실패: {}", e.getMessage());
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