package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzCommentResult(
        String commentId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static BuzzCommentResult from(BuzzComment comment) {
        return new BuzzCommentResult(
                comment.getCommentId(),
                new BuzzProfileResult(
                        comment.getCommenterId(),
                        comment.getCommenterNickname(),
                        comment.getCommenterProfileImageUrl()
                ),
                comment.getText(),
                comment.getImageUrls(),
                comment.getCreatedAt()
        );
    }
}
