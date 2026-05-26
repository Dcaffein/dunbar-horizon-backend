package com.example.DunbarHorizon.trace.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.trace.adapter.in.web.dto.VisitRequestDto;
import com.example.DunbarHorizon.trace.application.port.in.TraceCommandUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceCommandUseCase traceCommandUseCase;

    @PostMapping
    public ResponseEntity<Void> recordTrace(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody VisitRequestDto request) {

        traceCommandUseCase.recordTrace(currentUserId, request.targetId());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
