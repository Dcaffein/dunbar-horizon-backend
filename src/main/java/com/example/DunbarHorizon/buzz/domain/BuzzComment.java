package com.example.DunbarHorizon.buzz.domain;

import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuzzComment {

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

    public void update(String text, List<String> imageUrls) {
        validateContent(text);
        this.text = text;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    private void validateContent(String text) {
        if (text == null || text.isBlank()) {
            throw new BuzzInvalidStateException("댓글 내용은 비어있을 수 없습니다.");
        }
    }
}
