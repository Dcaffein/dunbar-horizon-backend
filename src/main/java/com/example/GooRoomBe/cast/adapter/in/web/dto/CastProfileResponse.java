package com.example.GooRoomBe.cast.adapter.in.web.dto;

import com.example.GooRoomBe.cast.application.port.out.dto.CastCreatorDto;

public record CastProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {
    public static CastProfileResponse from(CastCreatorDto dto) {
        return new CastProfileResponse(dto.id(), dto.nickname(), dto.profileImageUrl());
    }
}