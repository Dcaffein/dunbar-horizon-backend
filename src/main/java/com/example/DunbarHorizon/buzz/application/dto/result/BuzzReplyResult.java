package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.domain.BuzzReply;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzReplyResult(
        String replyId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static BuzzReplyResult from(BuzzReply reply) {
        return new BuzzReplyResult(
                reply.getReplyId(),
                new BuzzProfileResult(
                        reply.getReplierId(),
                        reply.getReplierNickname(),
                        reply.getReplierProfileImageUrl()
                ),
                reply.getText(),
                reply.getImageUrls(),
                reply.getCreatedAt()
        );
    }
}
