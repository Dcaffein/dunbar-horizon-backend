package com.example.GooRoomBe.social.trace.api;

import com.example.GooRoomBe.social.trace.api.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.social.trace.application.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @PostMapping("/{targetId}")
    public ResponseEntity<TraceRecordResponseDto> visitProfile(
            @AuthenticationPrincipal String visitorId,
            @PathVariable String targetId) {

        TraceRecordResponseDto response = traceService.visit(visitorId, targetId);

        return ResponseEntity.ok(response);
    }
}