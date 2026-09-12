package com.example.DunbarHorizon.flag.domain.flag;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;import com.example.DunbarHorizon.flag.domain.flag.event.FlagDeletedEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagExpiryExemptedEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagEncoreEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagMeetingChangedEvent;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagDeadlinePassedException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagFullCapacityException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidBasicInfoException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidCapacityException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.global.common.BaseTimeAggregateRoot;
import com.example.DunbarHorizon.global.common.SoftDeletable;
import com.example.DunbarHorizon.global.util.UuidUtil;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@DynamicUpdate
@Table(name = "flags", uniqueConstraints = {
        @UniqueConstraint(name = "uq_flags_parent_id", columnNames = "parent_id")
})
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Flag extends BaseTimeAggregateRoot implements SoftDeletable {

    public static final int EXPIRATION_THRESHOLD_HOURS = 24;

    public static final int TITLE_MAX_LENGTH = 20;
    public static final String TITLE_LENGTH_MESSAGE = "제목은 1자 이상 20자 이하로 입력해주세요.";
    public static final int DESCRIPTION_MAX_LENGTH = 500;
    public static final String DESCRIPTION_LENGTH_MESSAGE = "설명은 1자 이상 500자 이하로 입력해주세요.";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Getter
    private Long id;

    @Getter
    private Long hostId;

    @Getter
    private String title;

    @Column(columnDefinition = "TEXT") @Getter
    private String description;

    @Getter
    private Integer capacity;

    @Embedded @Getter
    private FlagSchedule schedule;

    @Getter
    @Column(columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID groupId;

    @Getter
    private Long parentId;

    @Getter
    private boolean autoExpiryExempt = false;

    @Getter
    private LocalDateTime deletedAt;

    private Flag(Long hostId, String title, String description, Integer capacity,
                 FlagSchedule schedule, Long parentId, UUID groupId) {
        validateBasicInfo(hostId, title, description);
        validateCapacity(capacity);

        this.hostId = hostId;
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.schedule = schedule;
        this.parentId = parentId;
        this.groupId = groupId;
    }

    public static Flag create(Long hostId, String title, String description,
                              Integer capacity, FlagSchedule schedule) {
        UUID newGroupId = UuidUtil.createV7();
        return new Flag(hostId, title, description, capacity, schedule, null, newGroupId);
    }

    Flag createEncore(Long hostId, LocalDateTime deadline, LocalDateTime start, LocalDateTime end) {
        if (!this.isEnded()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_NOT_ENDED, ErrorContext.of("flagId", id).and("status", calculateCurrentStatus()));
        }

        FlagSchedule newSchedule = FlagSchedule.of(deadline, start, end);
        Flag encoreFlag = new Flag(hostId, this.title, this.description,
                this.capacity, newSchedule, this.id, this.groupId);

        encoreFlag.registerEvent(new FlagEncoreEvent(this.id, hostId, this.title));
        return encoreFlag;
    }

    FlagParticipant participate(Long userId, int currentCount) {
        if (this.hostId.equals(userId)) {
            throw new FlagAuthorizationException(FlagErrorCode.FLAG_HOST_NOT_ELIGIBLE, ErrorContext.of("flagId", id).and("userId", userId));
        }

        if (!this.isRecruiting()) {
            throw new FlagDeadlinePassedException(id, schedule.getDeadline());
        }

        if (this.capacity != null && currentCount >= this.capacity) {
            throw new FlagFullCapacityException(id, capacity, currentCount);
        }

        return new FlagParticipant(this.id, userId);
    }

    void unparticipate(FlagParticipant participant, Long requesterId) {
        if (!participant.getParticipantId().equals(requesterId)) {
            throw new FlagAuthorizationException(FlagErrorCode.FLAG_SELF_PARTICIPATION_ONLY, ErrorContext.of("flagId", id).and("userId", requesterId));
        }

        if (!this.calculateCurrentStatus().isBeforeActivity()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_RECRUITMENT_CLOSED, ErrorContext.of("flagId", id));
        }
    }

    public void delete(Long requesterId) {
        validateHost(requesterId);
        if (isDeleted()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_ALREADY_DELETED, ErrorContext.of("flagId", id));
        }
        softDelete();
        registerEvent(new FlagDeletedEvent(
                this.id,
                this.hostId,
                this.parentId,
                this.title,
                calculateCurrentStatus()
        ));
    }

    public void updateBasicInfo(Long requesterId, String title, String description) {
        validateHost(requesterId);
        validateNotEnded();
        validateTitle(title);
        validateDescription(description);

        this.title = title;
        this.description = description;
    }

    public void updateCapacity(Long requesterId, Integer newCapacity, int currentParticipantCount) {
        validateHost(requesterId);
        validateNotEnded();
        validateNewCapacity(newCapacity, currentParticipantCount);

        this.capacity = newCapacity;
    }

    public void reschedule(Long requesterId, FlagSchedule newSchedule) {
        validateHost(requesterId);
        validateNotEnded();

        if (!calculateCurrentStatus().isBeforeActivity()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_ALREADY_STARTED, ErrorContext.of("flagId", id));
        }

        if (isMeetingTimeChanged(this.schedule, newSchedule)) {
            registerEvent(new FlagMeetingChangedEvent(this.id, this.title,
                    newSchedule.getStartDateTime(), newSchedule.getEndDateTime()));
        }

        this.schedule = newSchedule;
    }

    public void closeRecruitment(Long requesterId) {
        validateHost(requesterId);
        if (!isRecruiting()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_NOT_RECRUITING, ErrorContext.of("flagId", id).and("status", calculateCurrentStatus()));
        }

        this.schedule = this.schedule.withDeadline(LocalDateTime.now());
    }

    // 꺼졌다 다시 켜지면 다시 발행된다. 생애 최초 여부는 기록하지 않는다.
    void updateAutoExpiryExempt(boolean value) {
        if (value && !this.autoExpiryExempt) {
            registerEvent(new FlagExpiryExemptedEvent(this.id, this.hostId, this.parentId));
        }
        this.autoExpiryExempt = value;
    }

    public void severParentLink() {
        this.parentId = null;
    }

    public FlagStatus calculateCurrentStatus() {
        return schedule.calculateStatus(LocalDateTime.now());
    }

    public boolean isEnded() { return calculateCurrentStatus().isEnded(); }

    public boolean isRecruiting() { return calculateCurrentStatus().isRecruiting(); }

    public void grantInvitePermission(Long requesterId, FlagParticipant participant) {
        validateHost(requesterId);
        participant.grantInvitePermission();
    }

    public void revokeInvitePermission(Long requesterId, FlagParticipant participant) {
        validateHost(requesterId);
        participant.revokeInvitePermission();
    }

    private void validateHost(Long userId) {
        if (!this.hostId.equals(userId)) throw new FlagAuthorizationException(FlagErrorCode.FLAG_HOST_ONLY, ErrorContext.of("flagId", id).and("userId", userId));
    }

    private void validateNotEnded() {
        if (isEnded()) throw new FlagInvalidStatusException(FlagErrorCode.FLAG_ALREADY_ENDED, ErrorContext.of("flagId", id));
    }

    private void validateBasicInfo(Long hostId, String title, String description) {
        if (hostId == null) throw new FlagInvalidStatusException(FlagErrorCode.FLAG_HOST_REQUIRED);
        validateTitle(title);
        validateDescription(description);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > TITLE_MAX_LENGTH) {
            throw new FlagInvalidBasicInfoException(FlagErrorCode.FLAG_TITLE_LENGTH_INVALID);
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank() || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new FlagInvalidBasicInfoException(FlagErrorCode.FLAG_DESCRIPTION_LENGTH_INVALID);
        }
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null) return;
        if (capacity < 1) throw new FlagInvalidCapacityException(capacity);
    }

    private void validateNewCapacity(Integer newCapacity, int currentCount) {
        validateCapacity(newCapacity);

        if (newCapacity < currentCount) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_CAPACITY_BELOW_PARTICIPANTS,
                    ErrorContext.of("flagId", id).and("newCapacity", newCapacity).and("participantCount", currentCount));
        }
    }

    private boolean isMeetingTimeChanged(FlagSchedule oldSchedule, FlagSchedule newSchedule) {
        return !oldSchedule.getStartDateTime().equals(newSchedule.getStartDateTime()) ||
                !oldSchedule.getEndDateTime().equals(newSchedule.getEndDateTime());
    }

    @Override
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    @Override
    public boolean isDeleted() {
        return deletedAt != null;
    }

}