package com.example.DunbarHorizon.trace.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.trace.adapter.in.web.dto.VisitRequestDto;
import com.example.DunbarHorizon.trace.application.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @PostMapping
    public ResponseEntity<Void> recordTrace(
            @CurrentUserId Long currentUserId,
            @RequestBody VisitRequestDto request) {

        traceService.recordTrace(currentUserId, request.targetId());

        return ResponseEntity.ok().build();
    }
}
