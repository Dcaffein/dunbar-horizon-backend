package com.example.GooRoomBe.trace.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraceRecordResponseDto(
        boolean isMatched,
        Long targetId
) {
    public static TraceRecordResponseDto hidden() {
        return new TraceRecordResponseDto(false, null);
    }

    public static TraceRecordResponseDto revealed(Long targetId) {
        return new TraceRecordResponseDto(true, targetId);
    }
}