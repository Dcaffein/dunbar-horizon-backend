package com.example.DunbarHorizon.notification.application;

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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> saveNotifications(List<Notification> notifications) {
        return notificationRepository.saveAll(notifications);
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

    public void deleteNotification(Long notificationId, Long currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.requireOwnership(currentUserId);
        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void purgeExpiredNotifications() {
        int purged = notificationRepository.purgeExpired(LocalDateTime.now());

        if (purged > 0) {
            log.info("알림 정리 완료: 보관 기간이 지난 알림 {}건 삭제", purged);
        }
    }
}
