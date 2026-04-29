package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzCommentRequest;
import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzCreateRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buzzes")
@RequiredArgsConstructor
public class BuzzController {

    private final BuzzCommandUseCase buzzCommandUseCase;
    private final BuzzQueryUseCase buzzQueryUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createBuzz(
            @CurrentUserId Long currentUserId,
            @RequestPart("request") @Valid BuzzCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        buzzCommandUseCase.createBuzz(request.toCommand(currentUserId), images);
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

    @PostMapping(value = "/{buzzId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> comment(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @RequestPart("request") @Valid BuzzCommentRequest dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        buzzCommandUseCase.commentOnBuzz(currentUserId, buzzId, dto.text(), images, dto.isPublic());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping(value = "/{buzzId}/comments/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateComment(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable String commentId,
            @RequestPart("request") @Valid BuzzCommentRequest dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        buzzCommandUseCase.updateComment(currentUserId, buzzId, commentId, dto.text(), images);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{buzzId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable String commentId) {
        buzzCommandUseCase.deleteComment(currentUserId, buzzId, commentId);
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
