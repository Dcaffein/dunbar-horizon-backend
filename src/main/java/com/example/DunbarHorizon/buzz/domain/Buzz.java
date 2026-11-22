package com.example.DunbarHorizon.buzz.domain;

import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzErrorCode;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.global.util.UuidUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "buzzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Buzz {

    public static final int TEXT_MAX_LENGTH = 1000;
    public static final String TEXT_LENGTH_MESSAGE = "본문은 1자 이상 1000자 이하로 입력해주세요.";

    private static final Duration LIFETIME = Duration.ofHours(6);

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, updatable = false)
    private Long creatorId;

    @Column(nullable = false, updatable = false, length = TEXT_MAX_LENGTH)
    private String text;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private List<String> imageKeys;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    private Buzz(String id, Long creatorId, String text, List<String> imageKeys, LocalDateTime createdAt) {
        this.id = id;
        this.creatorId = creatorId;
        this.text = text;
        this.imageKeys = imageKeys;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plus(LIFETIME);
    }

    public static Buzz create(Long creatorId, String text, List<String> imageKeys,
                              Collection<Long> audienceIds, LocalDateTime now) {
        if (audienceIds == null || audienceIds.isEmpty()) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_RECIPIENTS_REQUIRED);
        }
        if (text == null || text.isBlank() || text.length() > TEXT_MAX_LENGTH) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_INVALID_TEXT);
        }
        List<String> keys = imageKeys != null ? List.copyOf(imageKeys) : List.of();
        return new Buzz(UuidUtil.createV7().toString(), creatorId, text, keys, now);
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isCreator(Long userId) {
        return creatorId.equals(userId);
    }

    public void validateNotExpired(LocalDateTime now) {
        if (isExpired(now)) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", id));
        }
    }

    public void validateThreadOpening(Long userId, boolean isAudienceMember, LocalDateTime now) {
        validateNotExpired(now);
        if (isCreator(userId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_CREATOR_CANNOT_OPEN_THREAD,
                    ErrorContext.of("buzzId", id).and("userId", userId));
        }
        validateAccess(userId, isAudienceMember);
    }

    public void validateAccess(Long userId, boolean isAudienceMember) {
        if (!isCreator(userId) && !isAudienceMember) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_NOT_RECIPIENT,
                    ErrorContext.of("buzzId", id).and("userId", userId));
        }
    }

    public void validateDeletion(Long requesterId) {
        if (!isCreator(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_CREATOR_ONLY,
                    ErrorContext.of("buzzId", id).and("userId", requesterId));
        }
    }
}
