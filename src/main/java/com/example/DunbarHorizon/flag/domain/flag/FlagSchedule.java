package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;import com.example.DunbarHorizon.flag.domain.flag.exception.FlagScheduleInvalidException;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class FlagSchedule {
    private LocalDateTime deadline;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private FlagSchedule(LocalDateTime deadline, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new FlagScheduleInvalidException(FlagErrorCode.FLAG_SCHEDULE_REQUIRED);
        }

        this.deadline = (deadline != null) ? deadline : start.minusMinutes(1);
        this.startDateTime = start;
        this.endDateTime = end;

        validateTimeOrder(this.deadline, start, end);
    }

    public static FlagSchedule of(LocalDateTime deadline, LocalDateTime start, LocalDateTime end) {
        return new FlagSchedule(deadline, start, end);
    }

    FlagSchedule withDeadline(LocalDateTime newDeadline) {
        return new FlagSchedule(newDeadline, this.startDateTime, this.endDateTime);
    }

    private void validateTimeOrder(LocalDateTime deadline, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null)
            throw new FlagScheduleInvalidException(FlagErrorCode.FLAG_SCHEDULE_REQUIRED);
        if (!end.isAfter(start))
            throw new FlagScheduleInvalidException(FlagErrorCode.FLAG_SCHEDULE_END_BEFORE_START,
                    ErrorContext.of("startDateTime", startDateTime).and("endDateTime", endDateTime));
        if (deadline != null && deadline.isAfter(start)) {
            throw new FlagScheduleInvalidException(FlagErrorCode.FLAG_SCHEDULE_DEADLINE_AFTER_START,
                    ErrorContext.of("deadline", deadline).and("startDateTime", startDateTime));
        }
    }

    public FlagStatus calculateStatus(LocalDateTime now) {
        if (now.isAfter(endDateTime)) return FlagStatus.ENDED;
        if (now.isAfter(startDateTime)) return FlagStatus.IN_ACTIVITY;

        if (now.isBefore(deadline)) return FlagStatus.RECRUITING;

        return FlagStatus.WAITING;
    }
}