package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzThreadOpenRequest;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadDetailResult;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadService;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buzzes/{buzzId}/threads")
@RequiredArgsConstructor
public class BuzzThreadController {

    private final BuzzThreadService buzzThreadService;

    @PostMapping
    public ResponseEntity<Void> openThread(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @RequestBody @Valid BuzzThreadOpenRequest request) {
        buzzThreadService.openThread(request.toCommand(currentUserId, buzzId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{threadId}")
    public ResponseEntity<BuzzThreadDetailResult> getThread(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable Long threadId) {
        return ResponseEntity.ok(buzzThreadService.getThread(currentUserId, buzzId, threadId));
    }
}
