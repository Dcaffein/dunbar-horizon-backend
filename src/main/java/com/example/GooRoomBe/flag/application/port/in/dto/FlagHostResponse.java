package com.example.GooRoomBe.flag.application.port.in.dto;

import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;

record FlagHostResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static FlagHostResponse from(FlagUserInfo info) {
        if (info == null) {
            return new FlagHostResponse(null, "알 수 없는 사용자", null);
        }
        return new FlagHostResponse(
                info.userId(),
                info.nickname(),
                info.profileImageUrl()
        );
    }
}
