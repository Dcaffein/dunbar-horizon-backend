package com.example.DunbarHorizon.buzz.domain;


import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzErrorCode;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import com.example.DunbarHorizon.global.exception.ErrorContext;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuzzComment {

    public static final int TEXT_MAX_LENGTH = 300;
    public static final String TEXT_LENGTH_MESSAGE = "댓글은 1자 이상 300자 이하로 작성해주세요.";

    private String commentId;

    private Long commenterId;

    private String commenterNickname;

    private String commenterProfileImageUrl;

    private String text;

    private List<String> imageUrls;

    private LocalDateTime createdAt;

    @Builder.Default
    private boolean isPublic = true;

    public static BuzzComment of(String commentId, Long commenterId, String nickname, String profileImageUrl,
                                 String text, List<String> imageUrls, boolean isPublic) {
        validateContent(text);
        return new BuzzComment(
                commentId,
                commenterId,
                nickname,
                profileImageUrl,
                text,
                imageUrls,
                LocalDateTime.now(),
                isPublic
        );
    }

    public boolean isCommenter(Long userId) {
        return commenterId.equals(userId);
    }

    public void update(Long requesterId, String text, List<String> imageUrls) {
        validateCommenter(requesterId);
        validateContent(text);
        this.text = text;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public void validateDeletionAuthority(Long requesterId, Long buzzCreatorId) {
        if (!isCommenter(requesterId) && !buzzCreatorId.equals(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_COMMENT_AUTHOR_ONLY,
                    ErrorContext.of("commentId", commentId).and("userId", requesterId));
        }
    }

    private void validateCommenter(Long requesterId) {
        if (!isCommenter(requesterId)) {
            throw new BuzzAccessDeniedException(BuzzErrorCode.BUZZ_COMMENT_AUTHOR_ONLY,
                    ErrorContext.of("commentId", commentId).and("userId", requesterId));
        }
    }

    private static void validateContent(String text) {
        if (text == null || text.isBlank() || text.length() > TEXT_MAX_LENGTH) {
            throw new BuzzInvalidStateException(BuzzErrorCode.BUZZ_COMMENT_INVALID_TEXT);
        }
    }
}
