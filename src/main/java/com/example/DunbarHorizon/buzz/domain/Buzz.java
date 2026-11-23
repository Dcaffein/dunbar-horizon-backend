package com.example.DunbarHorizon.buzz.domain;

import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzErrorCode;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import com.example.DunbarHorizon.global.exception.ErrorContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "buzzes")
@DynamicUpdate
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Buzz {

    public static final int TEXT_MAX_LENGTH = 1000;
    public static final String TEXT_LENGTH_MESSAGE = "본문은 1자 이상 1000자 이하로 입력해주세요.";

    public static final Duration LIFETIME = Duration.ofHours(6);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long creatorId;

    @Column(nullable = false, length = TEXT_MAX_LENGTH)
    private String text;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> imageKeys;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime editedAt;

    private LocalDateTime deletedAt;

    private Buzz(Long creatorId, String text, List<String> imageKeys, LocalDateTime createdAt) {
        this.creatorId = creatorId;
        this.text = text;
        this.imageKeys = imageKeys;
        this.createdAt = createdAt;
    }

    public static Buzz create(Long creatorId, String text, List<String> imageKeys,
                              Collection<Long> audienceIds, LocalDateTime now) {
        if (audienceIds == null || audienceIds.isEmpty()) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_RECIPIENTS_REQUIRED);
        }
        validateText(text);
        return new Buzz(creatorId, text, copyOf(imageKeys), now);
    }

    public void update(Long requesterId, String text, List<String> imageKeys, LocalDateTime now) {
        if (!isCreator(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_CREATOR_ONLY,
                    ErrorContext.of("buzzId", id).and("userId", requesterId));
        }
        validateText(text);
        this.text = text;
        this.imageKeys = copyOf(imageKeys);
        this.editedAt = now;
    }

    public boolean isCreator(Long userId) {
        return creatorId.equals(userId);
    }

    public void validateAccess(Long userId, boolean isAudienceMember) {
        validateAccess(id, creatorId, userId, isAudienceMember);
    }

    public static void validateAccess(Long buzzId, Long creatorId, Long userId, boolean isAudienceMember) {
        if (!creatorId.equals(userId) && !isAudienceMember) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_NOT_RECIPIENT,
                    ErrorContext.of("buzzId", buzzId).and("userId", userId));
        }
    }

    public void delete(Long requesterId, LocalDateTime now) {
        if (!isCreator(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_CREATOR_ONLY,
                    ErrorContext.of("buzzId", id).and("userId", requesterId));
        }
        this.deletedAt = now;
    }

    private static void validateText(String text) {
        if (text == null || text.isBlank() || text.length() > TEXT_MAX_LENGTH) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_INVALID_TEXT);
        }
    }

    private static List<String> copyOf(List<String> imageKeys) {
        return imageKeys != null ? List.copyOf(imageKeys) : List.of();
    }
}
