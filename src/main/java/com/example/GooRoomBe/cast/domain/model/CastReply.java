package com.example.GooRoomBe.cast.domain.model;

import com.example.GooRoomBe.cast.domain.exception.CastInvalidStateException;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CastReply {

    private String replyId;

    private Long replierId;

    private String replierNickname;

    private String replierProfileImageUrl;

    private String text;

    private List<String> imageUrls;

    private LocalDateTime createdAt;

    private boolean isPublic = true;

    public static CastReply of(String replyId, Long replierId, String nickname, String profileImageUrl,
                               String text, List<String> imageUrls, boolean isPublic) {
        return new CastReply(
                replyId,
                replierId,
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
            throw new CastInvalidStateException("답장 내용은 비어있을 수 없습니다.");
        }
    }
}
