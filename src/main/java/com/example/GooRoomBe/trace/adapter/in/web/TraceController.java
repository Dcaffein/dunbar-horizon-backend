package com.example.GooRoomBe.trace.adapter.in.web;

import com.example.GooRoomBe.global.annotation.CurrentUserId;
import com.example.GooRoomBe.trace.adapter.in.web.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.trace.adapter.in.web.dto.VisitRequestDto;
import com.example.GooRoomBe.trace.application.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @PutMapping
    public ResponseEntity<TraceRecordResponseDto> recordTrace(
            @CurrentUserId Long currentUserId,
            @RequestBody VisitRequestDto request) {

        TraceRecordResponseDto response = traceService.recordTrace(currentUserId, request.targetId());

        return ResponseEntity.ok(response);
    }
}