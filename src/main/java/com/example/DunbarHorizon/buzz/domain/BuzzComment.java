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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "buzz_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuzzComment {

    public static final int TEXT_MAX_LENGTH = 300;
    public static final String TEXT_LENGTH_MESSAGE = "답글은 1자 이상 300자 이하로 작성해주세요.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long threadId;

    @Column(nullable = false, updatable = false)
    private Long authorId;

    @Column(nullable = false, length = TEXT_MAX_LENGTH)
    private String text;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> imageKeys;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private BuzzComment(Long threadId, Long authorId, String text, List<String> imageKeys, LocalDateTime createdAt) {
        this.threadId = threadId;
        this.authorId = authorId;
        this.text = text;
        this.imageKeys = imageKeys;
        this.createdAt = createdAt;
    }

    public static BuzzComment write(Buzz buzz, BuzzThread thread, Long authorId, String text, List<String> imageKeys,
                                    LocalDateTime now) {
        if (buzz.isExpired(now)) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", buzz.getId()));
        }
        validateText(text);
        return new BuzzComment(thread.getId(), authorId, text, copyOf(imageKeys), now);
    }

    public boolean isAuthor(Long userId) {
        return authorId.equals(userId);
    }

    public void update(Long requesterId, String text, List<String> imageKeys) {
        if (!isAuthor(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_COMMENT_AUTHOR_ONLY,
                    ErrorContext.of("commentId", id).and("userId", requesterId));
        }
        validateText(text);
        this.text = text;
        this.imageKeys = copyOf(imageKeys);
    }

    public void validateDeletion(Long requesterId, Buzz buzz) {
        if (!isAuthor(requesterId) && !buzz.isCreator(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_COMMENT_DELETION_DENIED,
                    ErrorContext.of("commentId", id).and("userId", requesterId));
        }
    }

    private static void validateText(String text) {
        if (text == null || text.isBlank() || text.length() > TEXT_MAX_LENGTH) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_COMMENT_INVALID_TEXT);
        }
    }

    private static List<String> copyOf(List<String> imageKeys) {
        return imageKeys != null ? List.copyOf(imageKeys) : List.of();
    }
}
