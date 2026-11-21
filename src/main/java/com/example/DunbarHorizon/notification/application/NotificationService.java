package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.application.port.out.PushMessage;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.exception.NotificationNotFoundException;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenService deviceTokenService;
    private final NotificationSender notificationSender;

    public void send(List<Long> receiverIds, String title, String content,
                     NotificationType type, Map<String, Object> metadata) {
        List<Notification> notifications = receiverIds.stream()
                .map(receiverId -> Notification.builder()
                        .receiverId(receiverId)
                        .title(title)
                        .content(content)
                        .type(type)
                        .metadata(metadata)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);

        List<String> tokens = deviceTokenService.findTokensByUserIds(receiverIds);
        if (tokens.isEmpty()) {
            log.info("등록된 기기 토큰이 없어 푸시를 생략합니다. 대상: {}", receiverIds);
            return;
        }

        try {
            List<String> invalidTokens = notificationSender.send(pushMessage(title, content, type, metadata), tokens);
            deviceTokenService.cleanupInvalidTokens(invalidTokens);
        } catch (RuntimeException e) {
            log.error("푸시 발송 중 오류가 발생했습니다. 대상: {}", receiverIds, e);
        }
    }

    @Transactional
    public Notification readNotification(Long notificationId, Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = notificationRepository.findRetainedById(notificationId, now)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.read(currentUserId);
        return notificationRepository.save(notification);
    }

    public Slice<Notification> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findAllRetainedByReceiverId(userId, LocalDateTime.now(), pageable);
    }

    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countRetainedUnreadByReceiverId(userId, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.requireOwnership(currentUserId);
        notificationRepository.delete(notification);
    }

    @Transactional
    public void purgeExpiredNotifications() {
        int purged = notificationRepository.purgeExpired(LocalDateTime.now());

        if (purged > 0) {
            log.info("알림 정리 완료: 보관 기간이 지난 알림 {}건 삭제", purged);
        }
    }

    private PushMessage pushMessage(String title, String content, NotificationType type, Map<String, Object> metadata) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.name());
        if (metadata != null) {
            data.putAll(metadata);
        }
        return new PushMessage(title, content, data);
    }
}
