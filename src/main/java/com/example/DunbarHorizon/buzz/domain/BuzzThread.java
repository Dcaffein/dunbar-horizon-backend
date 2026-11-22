package com.example.DunbarHorizon.buzz.domain;

import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzErrorCode;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import com.example.DunbarHorizon.global.exception.ErrorContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "buzz_threads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuzzThread {

    public static final int TEXT_MAX_LENGTH = 300;
    public static final String TEXT_LENGTH_MESSAGE = "글은 1자 이상 300자 이하로 작성해주세요.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long buzzId;

    @Column(nullable = false, updatable = false)
    private Long starterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private ThreadVisibility visibility;

    @Column(nullable = false, length = TEXT_MAX_LENGTH)
    private String text;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> imageKeys;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private BuzzThread(Long buzzId, Long starterId, ThreadVisibility visibility,
                       String text, List<String> imageKeys, LocalDateTime createdAt) {
        this.buzzId = buzzId;
        this.starterId = starterId;
        this.visibility = visibility;
        this.text = text;
        this.imageKeys = imageKeys;
        this.createdAt = createdAt;
    }

    public static BuzzThread create(Buzz buzz, Long starterId, boolean isAudienceMember, ThreadVisibility visibility,
                                    String text, List<String> imageKeys, LocalDateTime now) {
        if (buzz.isExpired(now)) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", buzz.getId()));
        }
        if (buzz.isCreator(starterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_CREATOR_CANNOT_CREATE_THREAD,
                    ErrorContext.of("buzzId", buzz.getId()).and("userId", starterId));
        }
        if (!isAudienceMember) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_NOT_RECIPIENT,
                    ErrorContext.of("buzzId", buzz.getId()).and("userId", starterId));
        }
        if (visibility == null) {
            throw new IllegalArgumentException("스레드 공개 범위는 필수입니다.");
        }
        validateText(text);
        return new BuzzThread(buzz.getId(), starterId, visibility, text, copyOf(imageKeys), now);
    }

    public boolean isStarter(Long userId) {
        return starterId.equals(userId);
    }

    public boolean isPublic() {
        return visibility == ThreadVisibility.PUBLIC;
    }

    public boolean isAccessibleBy(Long viewerId, Buzz buzz, boolean isFriendOfStarter) {
        return isAccessibleBy(visibility, starterId, buzz.getCreatorId(), viewerId, isFriendOfStarter);
    }

    public static boolean isAccessibleBy(ThreadVisibility visibility, Long starterId, Long creatorId,
                                         Long viewerId, boolean isFriendOfStarter) {
        if (starterId.equals(viewerId) || creatorId.equals(viewerId)) {
            return true;
        }
        return visibility == ThreadVisibility.PUBLIC && isFriendOfStarter;
    }

    public boolean needsFriendshipToAccess(Long viewerId, Buzz buzz) {
        return needsFriendshipToAccess(visibility, starterId, buzz.getCreatorId(), viewerId);
    }

    public static boolean needsFriendshipToAccess(ThreadVisibility visibility, Long starterId, Long creatorId,
                                                  Long viewerId) {
        return visibility == ThreadVisibility.PUBLIC && !starterId.equals(viewerId) && !creatorId.equals(viewerId);
    }

    public void update(Long requesterId, String text, List<String> imageKeys) {
        if (!isStarter(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_THREAD_STARTER_ONLY,
                    ErrorContext.of("threadId", id).and("userId", requesterId));
        }
        validateText(text);
        this.text = text;
        this.imageKeys = copyOf(imageKeys);
    }

    public void validateDeletion(Long requesterId, Buzz buzz) {
        if (!isStarter(requesterId) && !buzz.isCreator(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_THREAD_DELETION_DENIED,
                    ErrorContext.of("threadId", id).and("userId", requesterId));
        }
    }

    public void validateAccess(Long viewerId, Buzz buzz, boolean isFriendOfStarter) {
        if (!isAccessibleBy(viewerId, buzz, isFriendOfStarter)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_THREAD_ACCESS_DENIED,
                    ErrorContext.of("threadId", id).and("userId", viewerId));
        }
    }

    static void validateText(String text) {
        if (text == null || text.isBlank() || text.length() > TEXT_MAX_LENGTH) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_THREAD_INVALID_TEXT);
        }
    }

    static List<String> copyOf(List<String> imageKeys) {
        return imageKeys != null ? List.copyOf(imageKeys) : List.of();
    }
}
