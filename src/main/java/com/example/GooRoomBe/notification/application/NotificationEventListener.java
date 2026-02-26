package com.example.GooRoomBe.notification.application;

import com.example.GooRoomBe.global.event.notification.NotificationEvent;
import com.example.GooRoomBe.notification.domain.Notification;
import com.example.GooRoomBe.notification.repository.NotificationRepository;
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

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationRequest(NotificationEvent event) {
        // 1. 공지사항 처리
        if (event.isAnnouncement()) {
            Notification notice = createSingleNotification(0L, event);
            notificationRepository.save(notice);
            fcmService.broadcastNotification(event, notice.getId());
            return;
        }

        // 2. 벌크 저장 (여러 명의 알림 내역을 한 번에 Batch Insert)
        List<Notification> notifications = event.receiverIds().stream()
                .map(id -> createSingleNotification(id, event))
                .toList();

        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        // 3. FCM 멀티캐스트 전송 위임
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