package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;

import java.time.LocalDateTime;

public record MemorialResult(
        Long id,
        Long writerId,
        String nickname,
        String profileImageUrl,
        String content,
        LocalDateTime createdAt
) {
    public static MemorialResult of(FlagMemorial memorial, FlagUserInfo writer) {
        return new MemorialResult(
                memorial.getId(),
                memorial.getWriterId(),
                writer.nickname(),
                writer.profileImageUrl(),
                memorial.getContent(),
                memorial.getCreatedAt()
        );
    }
}
