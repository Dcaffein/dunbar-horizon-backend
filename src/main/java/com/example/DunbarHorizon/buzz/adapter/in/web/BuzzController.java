package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzCommentRequest;
import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzCreateRequest;
import com.example.DunbarHorizon.buzz.application.service.BuzzService;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.global.imageStorage.PresignRequest;
import com.example.DunbarHorizon.global.imageStorage.PresignedUploadResult;
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

    private final BuzzService buzzService;
    private final ImageStoragePort imageStoragePort;

    @PostMapping("/images/presign")
    public ResponseEntity<List<PresignedUploadResult>> presignImages(
            @CurrentUserId Long currentUserId,
            @RequestBody List<PresignRequest> requests) {
        return ResponseEntity.ok(imageStoragePort.presignUploads(requests));
    }

    @PostMapping
    public ResponseEntity<Void> createBuzz(
            @CurrentUserId Long currentUserId,
            @RequestBody @Valid BuzzCreateRequest request) {
        List<String> imageKeys = request.imageKeys() != null ? request.imageKeys() : List.of();
        buzzService.createBuzz(request.toCommand(currentUserId), imageKeys);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/")
    public ResponseEntity<Slice<BuzzSummaryResult>> getReceivedBuzzes(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(buzzService.getReceivedBuzzes(currentUserId, pageable));
    }

    @GetMapping("/sent")
    public ResponseEntity<Slice<BuzzSummaryResult>> getSentBuzzes(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(buzzService.getSentBuzzes(currentUserId, pageable));
    }

    @GetMapping("/{buzzId}")
    public ResponseEntity<BuzzDetailResult> getBuzzDetail(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId) {
        return ResponseEntity.ok(buzzService.getBuzzDetail(currentUserId, buzzId));
    }

    @GetMapping("/senders/unread")
    public ResponseEntity<List<Long>> getUnreadSenders(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(buzzService.getUnreadSenderIds(currentUserId));
    }

    @PostMapping("/{buzzId}/comments")
    public ResponseEntity<Void> comment(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @RequestBody @Valid BuzzCommentRequest dto) {
        List<String> imageKeys = dto.imageKeys() != null ? dto.imageKeys() : List.of();
        buzzService.commentOnBuzz(currentUserId, buzzId, dto.text(), imageKeys, dto.isPublic());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{buzzId}/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable String commentId,
            @RequestBody @Valid BuzzCommentRequest dto) {
        List<String> imageKeys = dto.imageKeys() != null ? dto.imageKeys() : List.of();
        buzzService.updateComment(currentUserId, buzzId, commentId, dto.text(), imageKeys);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{buzzId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable String commentId) {
        buzzService.deleteComment(currentUserId, buzzId, commentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{buzzId}")
    public ResponseEntity<Void> deleteBuzz(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId) {
        buzzService.deleteBuzz(currentUserId, buzzId);
        return ResponseEntity.noContent().build();
    }
}
