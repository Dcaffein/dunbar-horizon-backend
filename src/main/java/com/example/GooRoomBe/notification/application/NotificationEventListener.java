package com.example.GooRoomBe.notification.application;

import com.example.GooRoomBe.global.event.NotificationEvent;
import com.example.GooRoomBe.notification.domain.Notification;
import com.example.GooRoomBe.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationRequest(NotificationEvent event) {

        //  먼저 DB에 기록 (isSent=false)
        Notification notification = Notification.builder()
                .receiverId(event.receiverId())
                .title(event.title())
                .content(event.content())
                .relatedUrl(event.relatedUrl())
                .type(event.type())
                .isSent(false)
                .build();

        notificationRepository.save(notification);

        //  FCM 전송 시도
        try {
            if ("ALL".equals(event.receiverId())) {
                // 전체 공지
                fcmService.sendToTopic("notice", event);
            } else {
                // 개인 알림
                fcmService.sendNotification(event);
            }

            // 성공 시 상태 변경
            notification.markAsSent();
            notificationRepository.save(notification);

            log.info("Notification Sent & Saved: {}", event);

        } catch (Exception e) {
            // DB에는 isSent=false로 남아있으므로, 나중에 스케줄러가 재시도 가능
            log.error("Failed to send FCM (saved in DB as pending): receiver={}", event.receiverId(), e);
        }
    }
}