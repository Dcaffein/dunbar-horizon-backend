package com.example.DunbarHorizon.trace.domain.model;

import com.example.DunbarHorizon.global.common.BaseTimeAggregateRoot;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.trace.domain.event.TraceRevealedEvent;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "trace", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trace_users", columnNames = {"user_a_id", "user_b_id"})
})
public class Trace extends BaseTimeAggregateRoot {

    private static final int REVEAL_THRESHOLD = 3;
    private static final int REVEALED_EXPIRATION_DAYS = 7;
    private static final int TRACING_EXPIRATION_DAYS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_a_id", nullable = false)
    private Long userAId;

    @Column(name = "user_b_id", nullable = false)
    private Long userBId;

    @Column(nullable = false)
    private int userACount;

    @Column(nullable = false)
    private int userBCount;

    private LocalDateTime userALastVisitedAt;
    private LocalDateTime userBLastVisitedAt;

    @Column(nullable = false)
    private boolean isRevealed;

    private LocalDateTime revealedAt;

    @Column(nullable = false)
    private LocalDateTime lastTracedAt;

    @Version
    private Long version;

    public record TraceIdPair(Long minId, Long maxId) {}

    public static TraceIdPair sortIds(Long id1, Long id2) {
        return id1 < id2 ? new TraceIdPair(id1, id2) : new TraceIdPair(id2, id1);
    }

    public Trace(Long visitorId, Long targetId) {
        validateNotSelf(visitorId, targetId);

        TraceIdPair sorted = sortIds(visitorId, targetId);
        this.userAId = sorted.minId();
        this.userBId = sorted.maxId();

        if (Objects.equals(visitorId, sorted.minId())) {
            this.userACount = 1;
            this.userBCount = 0;
            this.userALastVisitedAt = LocalDateTime.now();
        } else {
            this.userACount = 0;
            this.userBCount = 1;
            this.userBLastVisitedAt = LocalDateTime.now();
        }

        this.isRevealed = false;
        this.lastTracedAt = LocalDateTime.now();
    }

    public void recordVisit(Long visitorId) {
        if (isExpired()) {
            resetTrace();
        }

        LocalDateTime now = LocalDateTime.now();
        Long targetId;

        if (Objects.equals(this.userAId, visitorId)) {
            if (isAlreadyVisitedToday(this.userALastVisitedAt, now)) return;
            this.userACount++;
            this.userALastVisitedAt = now;
            targetId = this.userBId;
        } else if (Objects.equals(this.userBId, visitorId)) {
            if (isAlreadyVisitedToday(this.userBLastVisitedAt, now)) return;
            this.userBCount++;
            this.userBLastVisitedAt = now;
            targetId = this.userAId;
        } else {
            throw new IllegalArgumentException("Visitor is not a participant of this trace.");
        }

        this.lastTracedAt = now;

        registerEvent(new UserInteractionEvent(visitorId, targetId, InteractionType.VISIT));

        checkAndReveal();
    }

    private void resetTrace() {
        this.userACount = 0;
        this.userBCount = 0;
        this.userALastVisitedAt = null;
        this.userBLastVisitedAt = null;
        this.isRevealed = false;
        this.revealedAt = null;
        this.lastTracedAt = LocalDateTime.now();
        clearDomainEvents();
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();

        if (isRevealed && revealedAt != null) {
            return revealedAt.plusDays(REVEALED_EXPIRATION_DAYS).isBefore(now);
        }

        return lastTracedAt.plusDays(TRACING_EXPIRATION_DAYS).isBefore(now);
    }

    private void checkAndReveal() {
        if (!isRevealed && userACount >= REVEAL_THRESHOLD && userBCount >= REVEAL_THRESHOLD) {
            this.isRevealed = true;
            this.revealedAt = LocalDateTime.now();
            registerEvent(new TraceRevealedEvent(this.userAId, this.userBId));
        }
    }

    private boolean isAlreadyVisitedToday(LocalDateTime lastVisitedAt, LocalDateTime now) {
        if (lastVisitedAt == null) {
            return false;
        }
        return lastVisitedAt.toLocalDate().isEqual(now.toLocalDate());
    }

    private void validateNotSelf(Long visitorId, Long targetId) {
        if (Objects.equals(visitorId, targetId)) {
            throw new IllegalArgumentException("Cannot trace self.");
        }
    }
}