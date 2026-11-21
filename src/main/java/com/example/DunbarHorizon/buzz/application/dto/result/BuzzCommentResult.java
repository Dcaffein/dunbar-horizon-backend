package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzCommentResult(
        Long commentId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        LocalDateTime createdAt,
        boolean isMine
) {
    public static BuzzCommentResult of(BuzzComment comment, BuzzProfileResult author, List<String> imageUrls,
                                       Long viewerId) {
        return new BuzzCommentResult(
                comment.getId(),
                author,
                comment.getText(),
                imageUrls,
                comment.getCreatedAt(),
                comment.isAuthor(viewerId)
        );
    }
}
