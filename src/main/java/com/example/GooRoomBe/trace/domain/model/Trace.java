package com.example.GooRoomBe.trace.domain.model;

import com.example.GooRoomBe.global.event.interaction.InteractionType;
import com.example.GooRoomBe.global.event.interaction.UserInteractionEvent;
import com.example.GooRoomBe.trace.domain.event.TraceRevealedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Getter
@Table(name = "traces", uniqueConstraints = {
        @UniqueConstraint(name = "uk_visitor_target", columnNames = {"visitor_id", "target_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trace {

    public static final int REVEAL_THRESHOLD = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false)
    private Long visitorId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private Integer count;

    @Column(nullable = false)
    private LocalDateTime lastVisitedAt;

    public Trace(Long visitorId, Long targetId) {
        if (visitorId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신은 방문할 수 없습니다.");
        }
        this.visitorId = visitorId;
        this.targetId = targetId;
        this.count = 1;
        this.lastVisitedAt = LocalDateTime.now();
    }

    public void updateVisitCount() {
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(now)) {
            this.count = 1;
        } else if (isDifferentDay(now)) {
            this.count++;
        }
        this.lastVisitedAt = now;
    }

    public boolean isRevealReady() {
        return this.count >= REVEAL_THRESHOLD;
    }

    public Optional<TraceRevealedEvent> checkRevealEvent(int partnerCount) {
        if (this.isRevealReady() && partnerCount >= REVEAL_THRESHOLD) {
            return Optional.of(new TraceRevealedEvent(this.visitorId, this.targetId));
        }
        return Optional.empty();
    }

    public UserInteractionEvent createInteractionEvent() {
        return new UserInteractionEvent(
                this.visitorId,
                this.targetId,
                InteractionType.VISIT
        );
    }

    private boolean isExpired(LocalDateTime now) {
        return this.lastVisitedAt.plusDays(7).isBefore(now);
    }

    private boolean isDifferentDay(LocalDateTime now) {
        return !this.lastVisitedAt.toLocalDate().isEqual(now.toLocalDate());
    }
}