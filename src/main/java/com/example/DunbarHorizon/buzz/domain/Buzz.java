package com.example.DunbarHorizon.buzz.domain;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.buzz.domain.exception.BuzzErrorCode;import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzCommentNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import com.example.DunbarHorizon.global.util.UuidUtil;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "buzzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Buzz {

    public static final int TEXT_MAX_LENGTH = 1000;
    public static final String TEXT_LENGTH_MESSAGE = "본문은 1자 이상 1000자 이하로 입력해주세요.";

    @Id
    private String id;

    @Indexed
    private Long creatorId;

    private String creatorNickname;

    private String creatorProfileImageUrl;

    private String text;

    private List<String> imageUrls;

    @Indexed
    private List<Long> recipientIds = new ArrayList<>();

    private List<Long> readRecipientIds = new ArrayList<>();

    private List<BuzzComment> comments = new ArrayList<>();

    private LocalDateTime createdAt;

    @Indexed(expireAfter = "1s")
    private LocalDateTime expiresAt;

    @Builder
    public Buzz(Long creatorId, String creatorNickname, String creatorProfileImageUrl,
                String text, List<String> imageUrls, List<Long> recipientIds) {
        validateRecipientIds(recipientIds);
        validateText(text);

        this.creatorId = creatorId;
        this.creatorNickname = creatorNickname;
        this.creatorProfileImageUrl = creatorProfileImageUrl;
        this.text = text;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.recipientIds = recipientIds;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(6);
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isCreator(Long userId) {
        return creatorId.equals(userId);
    }

    public boolean isRecipient(Long userId) {
        return recipientIds.contains(userId);
    }

    public boolean isUnreadBy(Long userId) {
        return isRecipient(userId) && !readRecipientIds.contains(userId);
    }

    public void markAsRead(Long userId) {
        if (!isRecipient(userId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_NOT_RECIPIENT, ErrorContext.of("buzzId", id).and("userId", userId));
        }
        if (!readRecipientIds.contains(userId)) {
            this.readRecipientIds.add(userId);
        }
    }

    public BuzzComment createComment(Long commenterId, String nickname, String profileImageUrl,
                                     String text, List<String> imageUrls, boolean isPublic) {
        if (isExpired()) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", id));
        }
        if (!isRecipient(commenterId) && !isCreator(commenterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_NOT_RECIPIENT, ErrorContext.of("buzzId", id).and("userId", commenterId));
        }

        if (isRecipient(commenterId)) {
            markAsRead(commenterId);
        }

        return BuzzComment.of(
                UuidUtil.createV7().toString(),
                commenterId,
                nickname,
                profileImageUrl,
                text,
                imageUrls,
                isPublic
        );
    }

    public void updateComment(Long requesterId, String commentId, String newText, List<String> newImageUrls) {
        if (isExpired()) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", id));
        }

        BuzzComment target = findComments(commentId);

        if (!target.getCommenterId().equals(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_COMMENT_AUTHOR_ONLY, ErrorContext.of("buzzId", id).and("userId", requesterId));
        }

        target.update(newText, newImageUrls);
    }

    public void validateCommentDeletion(Long requesterId, String commentId) {
        if (isExpired()) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", id));
        }
        BuzzComment target = findComments(commentId);

        if (!target.getCommenterId().equals(requesterId) && !isCreator(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_COMMENT_AUTHOR_ONLY, ErrorContext.of("buzzId", id).and("userId", requesterId));
        }
    }

    public void validateAccess(Long userId) {
        if (!isRecipient(userId) && !isCreator(userId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_NOT_RECIPIENT, ErrorContext.of("buzzId", id).and("userId", userId));
        }
    }

    public void validateDeletion(Long requesterId) {
        if (!isCreator(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_CREATOR_ONLY, ErrorContext.of("buzzId", id).and("userId", requesterId));
        }
    }

    public void validateCommentCreation(Long commenterId) {
        if (isExpired()) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", id));
        }
        if (!isRecipient(commenterId) && !isCreator(commenterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_NOT_RECIPIENT, ErrorContext.of("buzzId", id).and("userId", commenterId));
        }
    }

    public void validateCommentUpdate(Long requesterId, String commentId) {
        if (isExpired()) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", id));
        }
        BuzzComment target = findComments(commentId);
        if (!target.getCommenterId().equals(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_COMMENT_AUTHOR_ONLY, ErrorContext.of("buzzId", id).and("userId", requesterId));
        }
    }

    public List<BuzzComment> getVisibleComments(Long viewerId) {
        return comments.stream()
                .filter(c -> c.isPublic() || isCreator(viewerId) || c.getCommenterId().equals(viewerId))
                .toList();
    }

    private BuzzComment findComments(String commentId) {
        return comments.stream()
                .filter(c -> c.getCommentId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new BuzzCommentNotFoundException(this.id, commentId));
    }

    private void validateRecipientIds(List<Long> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_RECIPIENTS_REQUIRED);
        }
        if (recipientIds.size() > 150) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_RECIPIENTS_EXCEEDED,
                    ErrorContext.of("recipientCount", recipientIds.size()));
        }
    }

    private void validateText(String text) {
        if (text == null || text.isBlank() || text.length() > TEXT_MAX_LENGTH) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_INVALID_TEXT);
        }
    }
}
