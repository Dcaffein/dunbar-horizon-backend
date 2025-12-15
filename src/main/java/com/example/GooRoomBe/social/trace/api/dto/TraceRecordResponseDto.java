package com.example.GooRoomBe.social.trace.api.dto;

import com.example.GooRoomBe.social.common.dto.SocialMemberResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraceRecordResponseDto(
        boolean isMatched,
        SocialMemberResponseDto partner
) {
    public static TraceRecordResponseDto hidden() {
        return new TraceRecordResponseDto(false, null);
    }

    public static TraceRecordResponseDto revealed(SocialMemberResponseDto partner) {
        return new TraceRecordResponseDto(true, partner);
    }
}