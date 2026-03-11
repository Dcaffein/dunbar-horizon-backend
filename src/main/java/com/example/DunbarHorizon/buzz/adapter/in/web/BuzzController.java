package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzCreateRequest;
import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzReplyRequest;
import com.example.DunbarHorizon.buzz.application.port.in.BuzzCommandUseCase;
import com.example.DunbarHorizon.buzz.application.port.in.BuzzQueryUseCase;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buzzes")
@RequiredArgsConstructor
public class BuzzController {

    private final BuzzCommandUseCase buzzCommandUseCase;
    private final BuzzQueryUseCase buzzQueryUseCase;

    @PostMapping
    public ResponseEntity<Void> createBuzz(
            @CurrentUserId Long currentUserId,
            @RequestBody @Valid BuzzCreateRequest request) {
        buzzCommandUseCase.createBuzz(request.toCommand(currentUserId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/")
    public ResponseEntity<Slice<BuzzSummaryResult>> getReceivedBuzzes(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(buzzQueryUseCase.getReceivedBuzzes(currentUserId, pageable));
    }

    @GetMapping("/{buzzId}")
    public ResponseEntity<BuzzDetailResult> getBuzzDetail(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId) {
        return ResponseEntity.ok(buzzQueryUseCase.getBuzzDetail(currentUserId, buzzId));
    }

    @GetMapping("/senders/unread")
    public ResponseEntity<List<Long>> getUnreadSenders(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(buzzQueryUseCase.getUnreadSenderIds(currentUserId));
    }

    @PostMapping("/{buzzId}/replies")
    public ResponseEntity<Void> reply(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @RequestBody @Valid BuzzReplyRequest dto) {
        buzzCommandUseCase.replyToBuzz(currentUserId, buzzId, dto.text(), dto.imageUrls(), dto.isPublic());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{buzzId}/replies/{replyId}")
    public ResponseEntity<Void> updateReply(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable String replyId,
            @RequestBody @Valid BuzzReplyRequest dto) {
        buzzCommandUseCase.updateReply(currentUserId, buzzId, replyId, dto.text(), dto.imageUrls());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{buzzId}/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable String replyId) {
        buzzCommandUseCase.deleteReply(currentUserId, buzzId, replyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{buzzId}")
    public ResponseEntity<Void> deleteBuzz(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId) {
        buzzCommandUseCase.deleteBuzz(currentUserId, buzzId);
        return ResponseEntity.noContent().build();
    }
}
