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
    public Slice<Notification> findAllRetainedByReceiverId(Long receiverId, LocalDateTime asOf, Pageable pageable) {
        return notificationJpaRepository.findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(
                receiverId, Notification.retentionCutoff(asOf), pageable);
    }

    @Override
    public long countRetainedUnreadByReceiverId(Long receiverId, LocalDateTime asOf) {
        return notificationJpaRepository.countByReceiverIdAndIsReadFalseAndCreatedAtAfter(
                receiverId, Notification.retentionCutoff(asOf));
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationJpaRepository.findById(id);
    }

    @Override
    public Optional<Notification> findRetainedById(Long id, LocalDateTime asOf) {
        return notificationJpaRepository.findByIdAndCreatedAtAfter(id, Notification.retentionCutoff(asOf));
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        return notificationJpaRepository.saveAll(notifications);
    }

    @Override
    public void deleteById(Long id) {
        notificationJpaRepository.deleteById(id);
    }

    @Override
    public int markAsRead(Long id, LocalDateTime asOf) {
        return notificationJpaRepository.updateIsReadTrueByIdAndCreatedAtAfter(id, Notification.retentionCutoff(asOf));
    }

    @Override
    public int purgeExpired(LocalDateTime asOf) {
        return notificationJpaRepository.deleteAllByCreatedAtAtOrBefore(Notification.retentionCutoff(asOf));
    }
}
