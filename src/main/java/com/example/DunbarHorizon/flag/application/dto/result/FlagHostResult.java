package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;

record FlagHostResult(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static FlagHostResult from(FlagUserInfo info) {
        if (info == null) {
            return new FlagHostResult(null, "알 수 없는 사용자", null);
        }
        return new FlagHostResult(
                info.userId(),
                info.nickname(),
                info.profileImageUrl()
        );
    }
}
