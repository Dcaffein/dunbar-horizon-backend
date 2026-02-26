package com.example.GooRoomBe.cast.adapter.in.web.dto;

import com.example.GooRoomBe.cast.domain.model.CastReply;

import java.time.LocalDateTime;
import java.util.List;

public record CastReplyResponseDto(
        String replyId,
        CastProfileResponse author,
        String text,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static CastReplyResponseDto from(CastReply reply) {
        return new CastReplyResponseDto(
                reply.getReplyId(),
                new CastProfileResponse(
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