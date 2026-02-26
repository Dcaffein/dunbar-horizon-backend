package com.example.GooRoomBe.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagParticipationDuplicateException extends FlagException {
    public FlagParticipationDuplicateException(Long flagId, Long userId) {
        super(
                String.format("사용자(ID: %d)는 이미 플래그(ID: %d)의 참여자로 등록되어 있습니다.", userId, flagId),
                HttpStatus.CONFLICT
        );
    }
}
