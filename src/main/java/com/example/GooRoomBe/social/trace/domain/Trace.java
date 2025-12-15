package com.example.GooRoomBe.social.trace.domain;

import com.example.GooRoomBe.global.event.InteractionType;
import com.example.GooRoomBe.global.event.UserInteractionEvent;
import com.example.GooRoomBe.social.socialUser.SocialUser;
import com.example.GooRoomBe.social.trace.domain.event.TraceRevealedEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Getter
@Node("Trace")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trace {

    public static final int REVEAL_THRESHOLD = 3;
    private static final double VISIT_WEIGHT = 1.0;

    @Id @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    @Relationship(type = "LEFT", direction = Relationship.Direction.INCOMING)
    private SocialUser visitor;

    @Relationship(type = "ON", direction = Relationship.Direction.OUTGOING)
    private SocialUser target;

    private Integer count;
    private LocalDateTime lastVisitedAt;

    public Trace(SocialUser visitor, SocialUser target) {
        if (visitor.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Self-visit is not allowed in Trace domain.");
        }

        this.id = UUID.randomUUID().toString();
        this.visitor = visitor;
        this.target = target;
        this.count = 1;
        this.lastVisitedAt = LocalDateTime.now();
    }

    protected Trace(String id, SocialUser visitor, SocialUser target, Integer count, LocalDateTime lastVisitedAt) {
        this.id = id;
        this.visitor = visitor;
        this.target = target;
        this.count = count;
        this.lastVisitedAt = lastVisitedAt;
    }

    public void updateVisitCount() {
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(now)) {
            this.count = 1;
        } else if (!isSameDay(now)) {
            this.count++;
        }
        this.lastVisitedAt = now;
    }

    //for test
    protected void updateVisitCount(LocalDateTime now) {
        if (isExpired(now)) {
            this.count = 1;
        } else if (!isSameDay(now)) {
            this.count++;
        }
        this.lastVisitedAt = now;
    }

    public boolean isRevealReady() {
        return this.count == REVEAL_THRESHOLD;
    }

    public Optional<TraceRevealedEvent> checkRevealEvent(int partnerCount) {
        if (this.count == REVEAL_THRESHOLD && partnerCount >= REVEAL_THRESHOLD) {
            return Optional.of(new TraceRevealedEvent(
                    this.visitor.getId(),
                    this.visitor.getNickname(),
                    this.target.getId()
            ));
        }
        return Optional.empty();
    }

    public UserInteractionEvent createInteractionEvent() {
        return new UserInteractionEvent(
                this.visitor.getId(),
                this.target.getId(),
                InteractionType.VISIT,
                VISIT_WEIGHT
        );
    }

    private boolean isExpired(LocalDateTime now) {
        return this.lastVisitedAt.plusDays(7).isBefore(now);
    }

    private boolean isSameDay(LocalDateTime now) {
        return this.lastVisitedAt.toLocalDate().isEqual(now.toLocalDate());
    }
}