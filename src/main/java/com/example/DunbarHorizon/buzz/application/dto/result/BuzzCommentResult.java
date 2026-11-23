package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.application.dto.view.BuzzCommentView;

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
    public static BuzzCommentResult of(BuzzCommentView comment, BuzzProfileResult author, List<String> imageUrls,
                                       Long viewerId) {
        return new BuzzCommentResult(
                comment.commentId(),
                author,
                comment.text(),
                imageUrls,
                comment.createdAt(),
                comment.authorId().equals(viewerId)
        );
    }
}
