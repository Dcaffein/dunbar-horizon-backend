package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagParticipationDuplicateException extends FlagException {
    public FlagParticipationDuplicateException(Long flagId, Long userId) {
        super(FlagErrorCode.FLAG_PARTICIPATION_DUPLICATE, String.format("사용자(ID: %d)는 이미 플래그(ID: %d)의 참여자로 등록되어 있습니다.", userId, flagId));
    }
}
