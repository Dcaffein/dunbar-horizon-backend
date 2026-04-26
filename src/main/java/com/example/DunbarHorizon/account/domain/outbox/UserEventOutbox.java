package com.example.DunbarHorizon.account.domain.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "user_event_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEventOutbox {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserOutboxEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserOutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public static UserEventOutbox pending(Long aggregateId, UserOutboxEventType eventType, String payload) {
        UserEventOutbox outbox = new UserEventOutbox();
        outbox.id = UUID.randomUUID().toString();
        outbox.aggregateId = aggregateId;
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.status = UserOutboxStatus.PENDING;
        outbox.retryCount = 0;
        outbox.createdAt = LocalDateTime.now();
        return outbox;
    }

    public void markCompleted() {
        this.status = UserOutboxStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public void markFailed() {
        this.status = UserOutboxStatus.FAILED;
        this.processedAt = LocalDateTime.now();
    }
}
