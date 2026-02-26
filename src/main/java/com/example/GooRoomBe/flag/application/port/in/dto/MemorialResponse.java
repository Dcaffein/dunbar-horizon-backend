package com.example.GooRoomBe.flag.application.port.in.dto;

import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;
import com.example.GooRoomBe.flag.domain.memorial.FlagMemorial;

import java.time.LocalDateTime;

public record MemorialResponse(
        Long id,
        Long writerId,
        String nickname,
        String profileImageUrl,
        String content,
        LocalDateTime createdAt
) {
    public static MemorialResponse of(FlagMemorial memorial, FlagUserInfo writer) {
        return new MemorialResponse(
                memorial.getId(),
                memorial.getWriterId(),
                writer.nickname(),
                writer.profileImageUrl(),
                memorial.getContent(),
                memorial.getCreatedAt()
        );
    }
}