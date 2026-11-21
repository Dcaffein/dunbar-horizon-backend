package com.example.DunbarHorizon.notification.domain;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.domain.exception.NotificationAccessDeniedException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    private static final Duration RETENTION_PERIOD = Duration.ofDays(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long receiverId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    private boolean isRead;

    private boolean isSent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static LocalDateTime retentionCutoff(LocalDateTime asOf) {
        return asOf.minus(RETENTION_PERIOD);
    }

    public void read(Long currentUserId) {
        if (!isOwnedBy(currentUserId)) {
            throw new NotificationAccessDeniedException(id, currentUserId);
        }
        this.isRead = true;
    }

    public void requireOwnership(Long currentUserId) {
        if (!isOwnedBy(currentUserId)) {
            throw new NotificationAccessDeniedException(id, currentUserId);
        }
    }

    private boolean isOwnedBy(Long userId) {
        return this.receiverId.equals(userId);
    }
}
