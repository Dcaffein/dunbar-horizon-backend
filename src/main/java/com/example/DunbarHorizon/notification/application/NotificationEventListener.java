package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationSender notificationSender;
    private final NotificationService historyService;
    private final NotificationSettingRepository settingRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationRequest(NotificationEvent event) {

        // 전체 공지사항 (Broadcast)
        if (event.isAnnouncement()) {
            Notification notice = createSingleNotification(null, event);
            notice = historyService.savePendingNotification(notice);

            notificationSender.sendBroadcast(event);
            historyService.markAsSent(notice.getId());
            return;
        }

        // 특정 사용자 알림 (Multicast)
        // 1. 수신자 설정 조회 및 유효한 토큰 추출 (DB 읽기)
        List<NotificationSetting> settings = settingRepository.findAllByUserIdIn(event.receiverIds());
        List<String> validTokens = settings.stream()
                .filter(NotificationSetting::isAlarmOn) // 알림 켜진 사람만
                .map(NotificationSetting::getFcmToken)
                .filter(Objects::nonNull) // 토큰이 있는 사람만
                .toList();

        if (validTokens.isEmpty()) {
            log.info("유효한 FCM 토큰이 없어 알림 발송을 생략합니다. 대상: {}", event.receiverIds());
            return; // 굳이 빈 토큰으로 진행할 필요 없음
        }

        // 2. 알림 내역 DB 저장 (Tx)
        List<Notification> notifications = event.receiverIds().stream()
                .map(id -> createSingleNotification(id, event))
                .toList();
        List<Notification> savedNotifications = historyService.savePendingNotifications(notifications);

        try {
            // 3. FCM 발송
            List<String> invalidTokens = notificationSender.sendMulticast(event, validTokens);

            // 4. 결과 업데이트 및 죽은 토큰 청소 (Tx)
            historyService.markAllAsSent(savedNotifications);
            historyService.cleanupInvalidTokens(invalidTokens);

        } catch (Exception e) {
            log.error("FCM 벌크 전송 중 시스템 에러: {}", e.getMessage());
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