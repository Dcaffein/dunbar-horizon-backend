package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.notification.adapter.out.persistence.jpa.NotificationJpaRepository;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public Slice<Notification> findAllRetainedByReceiverId(Long receiverId, LocalDateTime now, Pageable pageable) {
        return notificationJpaRepository.findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(
                receiverId, Notification.retentionCutoff(now), pageable);
    }

    @Override
    public long countRetainedUnreadByReceiverId(Long receiverId, LocalDateTime now) {
        return notificationJpaRepository.countByReceiverIdAndIsReadFalseAndCreatedAtAfter(
                receiverId, Notification.retentionCutoff(now));
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationJpaRepository.findById(id);
    }

    @Override
    public Optional<Notification> findRetainedById(Long id, LocalDateTime now) {
        return notificationJpaRepository.findByIdAndCreatedAtAfter(id, Notification.retentionCutoff(now));
    }

    @Override
    public Notification save(Notification notification) {
        return notificationJpaRepository.save(notification);
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        return notificationJpaRepository.saveAll(notifications);
    }

    @Override
    public void delete(Notification notification) {
        notificationJpaRepository.deleteById(notification.getId());
    }

    @Override
    public int purgeExpired(LocalDateTime now) {
        return notificationJpaRepository.deleteAllByCreatedAtAtOrBefore(Notification.retentionCutoff(now));
    }
}
