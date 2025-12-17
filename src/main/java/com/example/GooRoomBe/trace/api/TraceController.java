package com.example.GooRoomBe.trace.api;

import com.example.GooRoomBe.trace.api.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.trace.api.dto.VisitRequestDto;
import com.example.GooRoomBe.trace.application.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @PostMapping
    public ResponseEntity<TraceRecordResponseDto> visitProfile(
            @AuthenticationPrincipal String visitorId,
            @RequestBody VisitRequestDto request) {

        TraceRecordResponseDto response = traceService.visit(visitorId, request.targetId());

        return ResponseEntity.ok(response);
    }
}